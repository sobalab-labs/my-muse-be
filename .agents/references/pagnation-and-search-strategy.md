# Search and Pagination Spec (PostgreSQL-only)

Audience: coding agents. Rules are normative. Follow them unless a later section overrides.

## 0. Constraints

- Search targets: artist names and concert titles only. Short strings (typically under 40 chars). No sentences.
- Languages: Korean (ko), Japanese (ja), English (en). Mixed-script input is possible.
- Required: typo tolerance and cross-language tolerance (e.g. `블랙핑크` / `BLACKPINK` / `ブラックピンク` should find the same artist).
- Stack: PostgreSQL only. No Elasticsearch/OpenSearch. Search sits behind a `SearchService` interface so an engine can replace it later.
- Pre-production, small data. Optimize for correctness and simplicity, not scale.

## 1. Key design decisions

1. **Cross-language matching is data-driven, not algorithmic.** Trigram similarity cannot match across scripts. Store explicit per-language names and aliases for every entity. Romanization is only a weak fallback (see 3.3).
2. **One row per searchable term variant** in a `search_term` table. Search runs against this table, then groups by entity.
3. **Normalize identically at index time and query time** (section 3.1). Most match failures come from skipping this.
4. **Typo tolerance via `pg_trgm`** on normalized terms. For Korean, also index a jamo-decomposed variant, because a one-jamo typo otherwise breaks a whole syllable and destroys trigram similarity on short names.
5. **Keyword search = relevance sort with a capped result depth.** **No keyword = keyset pagination.** Do not mix the two cursor types.

## 2. Schema

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE search_term (
  id          bigserial PRIMARY KEY,
  entity_type text   NOT NULL,   -- 'artist' | 'concert'
  entity_id   bigint NOT NULL,
  kind        text   NOT NULL,   -- 'name' | 'alias' | 'romanized' | 'jamo'
  lang        text,              -- 'ko' | 'ja' | 'en' | NULL
  term_norm   text   NOT NULL,   -- normalized per 3.1
  UNIQUE (entity_type, entity_id, kind, term_norm)
);

CREATE INDEX idx_search_term_trgm ON search_term USING gin (term_norm gin_trgm_ops);
CREATE INDEX idx_search_term_entity ON search_term (entity_type, entity_id);

-- Browse-mode pagination index (on each entity table; shown for concert)
CREATE INDEX idx_concert_created_id ON concert (created_at DESC, id DESC);
```

Preconditions:
- Database must be UTF-8 with a non-`C` locale. Verify: `SELECT show_trgm('콘서트');` returns trigrams. Empty or near-empty means the locale is wrong.
- Source entities hold multilingual fields (e.g. `name_ko`, `name_ja`, `name_en`, plus an `aliases` list). The aliases are the main source of cross-language matches. Populate them at ingest (manual curation or an external dataset).

## 3. Indexing and normalization

### 3.1 Normalization function `norm(text)` (apply to every indexed term and every query)

1. Unicode NFKC (folds full-width/half-width, compatibility forms; also resolves NFD Hangul to NFC).
2. Lowercase / case-fold.
3. Katakana to hiragana fold (so `ブラック` equals `ぶらっく`).
4. Remove whitespace and punctuation (so `BLACK PINK`, `Black-Pink`, `blackpink` are equal).
5. Trim. Reject or truncate queries over 50 chars.

Keep the original display string separately. Never search against it.

### 3.2 Variants to generate per entity

| kind | content | purpose |
|---|---|---|
| `name` | `norm(name_ko)`, `norm(name_ja)`, `norm(name_en)` | primary match per language |
| `alias` | `norm(alias)` for each alias (any language, abbreviations, nicknames) | cross-language and common-spelling matches |
| `jamo` | Hangul terms decomposed to jamo, then normalized | typo tolerance for Korean; also supports 초성-style prefix search |
| `romanized` | Latin transliteration of ko/ja terms | weak fallback only (3.3) |

Skip `jamo` for non-Hangul terms.

### 3.3 Romanization caveat

Transliteration does not reproduce the official English spelling (Korean "블랙핑크" romanizes to something like "beullaekpingkeu", not "blackpink"). Therefore:
- Do not rely on `romanized` for brand-style names. Use explicit `alias`/`name_en` data.
- `romanized` is useful for native names (e.g. Japanese names written in kana, searched in romaji).
- ICU4J `Transliterator` can produce Latin from kana and Hangul. Kanji conversion through it yields Chinese-style readings, not Japanese readings, so do not trust it for kanji. Verify behavior on real data before shipping.

## 4. Search query (keyword present)

Run the query once per query form, `UNION ALL`, then aggregate per entity:

- Form 1: `norm(q)`
- Form 2: jamo-decomposed `norm(q)`, only if `q` contains Hangul
- Form 3: romanized `norm(q)`, optional, only if `romanized` terms are enabled

```sql
-- :q is one query form, already normalized and LIKE-escaped
SELECT entity_type, entity_id, MAX(score) AS score
FROM (
  SELECT entity_type, entity_id,
    CASE
      WHEN term_norm = :q                    THEN 1.0
      WHEN term_norm LIKE :q || '%'          THEN 0.9
      WHEN term_norm LIKE '%' || :q || '%'   THEN 0.7
      ELSE similarity(term_norm, :q)
    END AS score
  FROM search_term
  WHERE term_norm LIKE '%' || :q || '%'      -- substring, GIN-assisted
     OR term_norm % :q                       -- typo tolerance, GIN-assisted
  -- UNION ALL the same SELECT for the other query forms
) t
GROUP BY entity_type, entity_id
ORDER BY score DESC, entity_id DESC          -- id is the deterministic tiebreaker
LIMIT :limitPlusOne OFFSET :offset;
```

Rules:
- Escape `%`, `_`, `\` in user input.
- Set `pg_trgm.similarity_threshold` per transaction (`SET LOCAL`). Start at 0.3. Short names have few trigrams, so thresholds too low give noise and too high miss typos. Tune against the test set in section 8.
- Join back to the entity tables to fetch display fields after ranking.
- Optional filter `type=artist|concert` adds `AND entity_type = :type`.
- Query length 1 to 2 chars: allowed. The substring/prefix branch handles it; trigram similarity will be weak. Do not enforce a minimum length for ko/ja.

## 5. Pagination

### 5.1 Keyword mode (relevance)

- Scores are computed per query and not indexed, so keyset on the index does not apply.
- Use `LIMIT/OFFSET` with a hard depth cap: `offset + limit <= 100`. Beyond the cap, return `hasNext=false`.
- Page size default 20, max 50. Fetch `limit + 1` rows to compute `hasNext` (no `COUNT(*)`).
- The `id` tiebreaker in `ORDER BY` is mandatory for stable pages.

### 5.2 Browse mode (no keyword)

Keyset on the entity table:

```sql
SELECT ... FROM concert
WHERE (:lastCreatedAt IS NULL OR (created_at, id) < (:lastCreatedAt, :lastId))
ORDER BY created_at DESC, id DESC
LIMIT :limitPlusOne;
```

- Sort columns and index direction must match exactly.
- Never use `OFFSET` in this mode.

### 5.3 Response and cursor

```java
public record CursorResponse<T>(List<T> items, String nextCursor, boolean hasNext) {}
```

- `nextCursor`: opaque Base64URL token. Browse mode encodes `(createdAt, id)`. Keyword mode encodes `offset` only.
- Embed a hash of `(q, sort, type)` in the cursor. On mismatch with the current request, return 400.
- Do not return `totalCount` by default.

## 6. Endpoint

```
GET /api/search?q=<str>&type=<artist|concert>&limit=20&cursor=<opaque>
```

- `q` empty or missing: browse mode (5.2).
- `q` present: keyword mode (5.1).
- Optional separate lightweight `GET /api/search/suggest?q=` for autocomplete: prefix + trigram, top 5 to 10, no cursor.

## 7. Frontend requirements

- Debounce 200 to 300 ms; cancel stale requests.
- IME: do not search during Korean/Japanese composition. Check `isComposing`, search on `compositionend`.
- Query in URL; infinite scroll uses `nextCursor` while `hasNext`.

## 8. Required tests

Create a fixture of real-looking artists/concerts with ko/ja/en names and aliases, then assert:

- Cross-language: ko query finds en-named entity and vice versa (via aliases); ja katakana query finds the same entity.
- Spacing/case/punctuation: `BLACK PINK`, `black-pink`, `blackpink` return the same result.
- Katakana vs hiragana input returns the same result.
- Hangul typo (one wrong jamo) still returns the intended entity (via `jamo` variant).
- English typo (one dropped or swapped letter) returns the intended entity.
- NFD-composed Hangul input matches.
- 1-char Korean query returns results without error.
- Pagination: ties on score and on `created_at` produce no duplicates or gaps; keyword depth cap enforced; mismatched cursor returns 400.
- `EXPLAIN (ANALYZE, BUFFERS)` shows `idx_search_term_trgm` used on a table sized to expected data.

## 9. Maintenance

- Regenerate `search_term` rows in the same transaction as entity writes (single DB, no sync problem). Delete and re-insert variants on update.
- Changing `norm()` requires a full rebuild of `search_term`. Version the function.

## 10. Upgrade trigger

Replace the Postgres implementation (new `SearchService` implementation, Postgres stays source of truth) if any of: relevance complaints that tuning cannot fix, need for true morphological matching or large synonym sets, or latency problems on correctly indexed queries at large scale. Candidates: OpenSearch with Nori, Meilisearch, Typesense.
