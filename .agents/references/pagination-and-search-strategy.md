# Search and Pagination Spec (PostgreSQL-only)

Audience: coding agents. Rules are normative. Follow them unless a later section overrides.

## 0. Constraints

- Search targets: artist names and concert titles only. Short strings (typically under 40 chars). No sentences.
- Languages: Korean (ko), Japanese (ja), English (en). Mixed-script input is possible.
- Required: typo tolerance and cross-language tolerance (e.g. `블랙핑크` / `BLACKPINK` / `ブラックピンク` should find the same artist).
- Stack: PostgreSQL only. No Elasticsearch/OpenSearch, no Redis. Search sits behind a `SearchService` interface so an engine can replace it later.
- Pre-production, small data. Optimize for correctness and simplicity, not scale.
- Caching: out of scope for now. Do not add a cache layer. Measure first (see 10).

## 1. Key design decisions

1. **Cross-language matching is data-driven, not algorithmic.** Trigram similarity cannot match across scripts. Store explicit per-language names and aliases for every entity. Romanization is only a weak fallback (see 3.3).
2. **One row per searchable term variant** in a `search_term` table. Search runs against this table, then groups by entity.
3. **Normalize identically at index time and query time** (section 3.1).
4. **Typo tolerance via `pg_trgm`** on normalized terms. For Korean, also index a jamo-decomposed variant.
5. **Search is one aggregate endpoint** returning grouped results: a ranked **artist list** and a **concert list**. The client never orchestrates separate artist and concert calls for the search screen.
6. **Resolve artists once, pin their IDs and scores, then pass IDs not strings.** Artist matching runs on the first page only. Concert scroll requests use a signed cursor carrying the pinned artists and never repeat artist matching.
7. **Concert list = one merged query**: concerts of the pinned artists OR concerts whose title matches. The DB deduplicates and ranks. Do not run the two sets as separate queries merged in application code.
8. **Search is keyword-only.** Relevance sort with OFFSET and a capped depth. There is no browse (no-keyword) mode; a plain listing, if ever needed, is a separate endpoint.

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

-- Many-to-many: a concert can have multiple artists
CREATE TABLE concert_artist (
  concert_id bigint NOT NULL REFERENCES concert(id),
  artist_id  bigint NOT NULL REFERENCES artist(id),
  PRIMARY KEY (concert_id, artist_id)
);
CREATE INDEX idx_concert_artist_artist ON concert_artist (artist_id, concert_id);
```

Preconditions:
- Database must be UTF-8 with a non-`C` locale. Verify: `SELECT show_trgm('콘서트');` returns trigrams. Empty or near-empty means the locale is wrong.
- Source entities hold multilingual fields (e.g. `name_ko`, `name_ja`, `name_en`, plus an `aliases` list). Aliases are the main source of cross-language matches. Populate them at ingest.

## 3. Indexing and normalization

### 3.1 Normalization function `norm(text)` (apply to every indexed term and every query)

1. Unicode NFKC (also resolves NFD Hangul to NFC).
2. Lowercase / case-fold.
3. Katakana to hiragana fold.
4. Remove whitespace and punctuation.
5. Trim. Reject or truncate queries over 50 chars.

Keep the original display string separately. Never search against it.

### 3.2 Variants to generate per entity

| kind | content | purpose |
|---|---|---|
| `name` | `norm(name_ko)`, `norm(name_ja)`, `norm(name_en)` | primary match per language |
| `alias` | `norm(alias)` for each alias (any language, abbreviations, nicknames) | cross-language and common-spelling matches |
| `jamo` | Hangul terms decomposed to jamo, then normalized | Korean typo tolerance; 초성-style prefix search |
| `romanized` | Latin transliteration of ko/ja terms | weak fallback only (3.3) |

Skip `jamo` for non-Hangul terms.

### 3.3 Romanization caveat

Transliteration does not reproduce official English spellings ("블랙핑크" becomes something like "beullaekpingkeu", not "blackpink"). Therefore:
- Do not rely on `romanized` for brand-style names. Use explicit `alias`/`name_en` data.
- `romanized` is useful for native names (e.g. Japanese kana names searched in romaji).
- ICU4J `Transliterator` handles kana and Hangul. Kanji yields Chinese-style readings; do not trust it for kanji. Verify on real data.

## 4. Term search (reusable building block)

`termSearch(entityType, q, minScore)` returns `(entity_id, score)` rows. Run once per query form, `UNION ALL`, then aggregate per entity:

- Form 1: `norm(q)`
- Form 2: jamo-decomposed `norm(q)`, only if `q` contains Hangul
- Form 3: romanized `norm(q)`, optional, only if `romanized` terms are enabled

```sql
-- :q is one query form, already normalized and LIKE-escaped
SELECT entity_id, MAX(score) AS score
FROM (
  SELECT entity_id,
    CASE
      WHEN term_norm = :q                    THEN 1.0
      WHEN term_norm LIKE :q || '%'          THEN 0.9
      WHEN term_norm LIKE '%' || :q || '%'   THEN 0.7
      ELSE similarity(term_norm, :q)
    END AS score
  FROM search_term
  WHERE entity_type = :entityType
    AND (term_norm LIKE '%' || :q || '%'     -- substring, GIN-assisted
      OR term_norm % :q)                     -- typo tolerance, GIN-assisted
  -- UNION ALL the same SELECT for the other query forms
) t
GROUP BY entity_id
HAVING MAX(score) >= :minScore
```

Rules:
- Escape `%`, `_`, `\` in user input.
- Set `pg_trgm.similarity_threshold` per transaction (`SET LOCAL`). Start at 0.3. Tune against section 9 tests.
- Query length 1 to 2 chars: allowed; the substring/prefix branch handles it. Do not enforce a minimum length for ko/ja.

## 5. Aggregate search flow (first page)

Execute sequentially. Do not parallelize (the concert query depends on the artist result; the artist lookup is cheap).

Config:
- `ARTIST_MATCH_THRESHOLD` (default `0.5`): min score for an artist to be listed. Tune via section 9 tests (one-typo matches may score below it).
- `ARTIST_PAGE_SIZE` (default `10`, max `20`).

**Step 1. Match artists.**
`termSearch('artist', q, ARTIST_MATCH_THRESHOLD)` with `ORDER BY score DESC, entity_id DESC LIMIT :artistPageSize+1 OFFSET 0`.
- Return these as the artist list (first page).
- `pinnedArtists` = the first `ARTIST_PAGE_SIZE` rows as `[(artistId, score), ...]` (may be empty).

**Step 2. Merged concert query (single query).**

```sql
SELECT concert_id, MAX(score) AS score
FROM (
  -- title matches
  SELECT entity_id AS concert_id, score
  FROM ( /* termSearch('concert', q, 0) as in section 4 */ ) title_hits
  UNION ALL
  -- concerts of pinned artists, scored by the artist's match score
  -- (skip this branch when pinnedArtists is empty)
  SELECT ca.concert_id, a.score
  FROM concert_artist ca
  JOIN unnest(:artistIds::bigint[], :artistScores::float8[]) AS a(artist_id, score)
    ON a.artist_id = ca.artist_id
) m
GROUP BY concert_id                       -- GROUP BY is the dedupe
ORDER BY score DESC, concert_id DESC      -- id tiebreaker is mandatory
LIMIT :limitPlusOne OFFSET :offset;
```

- A concert matched by several routes appears once with its highest score.
- Join back to `concert` (and `concert_artist`/`artist` for display) after ranking.
- `types` filter: `types=concert` still runs Step 1 for pinning but omits `artists` from the response; `types=artist` skips Step 2.

## 6. Pagination

### 6.1 Rules

- First page: `/api/search` (section 7). Later pages: `/api/search/artists` and `/api/search/concerts` with `cursor` only (no `q`; concert pages do no artist matching).
- `LIMIT/OFFSET` with hard depth cap per list: `offset + limit <= 100`. Beyond the cap return `hasNext=false`.
- Concert page size default 20, max 50. Artist page size per `ARTIST_PAGE_SIZE`. Fetch `limit + 1` rows to compute `hasNext`. No `COUNT(*)`.
- Artist list scrolling is independent of pinning: the concert list stays pinned to the first-page artists.

### 6.2 Cursor

Opaque Base64URL JSON, **HMAC-signed**. Reject tampered, wrong-version, or wrong-endpoint cursors with 400. `t` must match the endpoint (`artist` for `/artists`, `concert` for `/concerts`).

```json
// concert
{ "t": "concert", "q": "<norm(q)>", "artists": [[123, 0.9], [456, 0.6]], "offset": 20, "limit": 20, "v": 1 }
// artist
{ "t": "artist", "q": "<norm(q)>", "offset": 10, "limit": 10, "v": 1 }
```

- The concert cursor carries the normalized `q` and pinned `(artistId, score)` pairs (at most `ARTIST_PAGE_SIZE`), so artist matching never re-runs and the pinned set cannot change mid-scroll.
- Do not return `totalCount`.

## 7. Endpoints

### 7.1 `GET /api/search?q=<str>&types=artist,concert&limit=20`

- `q` is required. Empty, blank (empty after `norm()`), or missing `q` returns 400.
- `types` default `artist,concert`. `limit` applies to concerts.

```java
public record SearchResponse(
    CursorResponse<ArtistDto> artists,   // first page, up to ARTIST_PAGE_SIZE
    CursorResponse<ConcertDto> concerts  // first page
) {}
public record CursorResponse<T>(List<T> items, String nextCursor, boolean hasNext) {}
```

### 7.2 `GET /api/search/artists?cursor=<opaque>`
### 7.3 `GET /api/search/concerts?cursor=<opaque>&limit=20`

- Subsequent pages only. Return `CursorResponse<T>`. `cursor` required. `/concerts` `limit` may be overridden within max.

### 7.4 `GET /api/search/suggest?q=` (optional)

Autocomplete: prefix + trigram, top 5 to 10, no cursor, no pinning.

## 8. Frontend requirements

- Debounce 200 to 300 ms; cancel stale requests.
- IME: do not search during Korean/Japanese composition. Check `isComposing`, search on `compositionend`.
- Query in URL. Initial render uses `/api/search`. Each list scrolls independently using its own `nextCursor` while `hasNext`.
- Dedupe appended items by id. A new `q` resets both lists and cursors.

## 9. Required tests

Fixture: real-looking artists/concerts with ko/ja/en names and aliases, including multi-artist concerts, several artists matching one query, and a concert whose title equals an artist name.

- Cross-language: ko query finds en-named entity and vice versa (via aliases); ja katakana query finds the same entity.
- Spacing/case/punctuation: `BLACK PINK`, `black-pink`, `blackpink` return the same result.
- Katakana vs hiragana input returns the same result.
- Hangul typo (one wrong jamo) still returns the intended entity (`jamo` variant); verify it passes `ARTIST_MATCH_THRESHOLD`, else tune.
- English typo (dropped or swapped letter) returns the intended entity.
- NFD-composed Hangul input matches.
- 1-char Korean query returns results without error.
- Missing, empty, or punctuation-only `q` returns 400.
- Artist list: all artists >= threshold are listed, ordered by score then id; below-threshold artists are excluded; first page capped at `ARTIST_PAGE_SIZE`.
- Pinning: concerts of every pinned artist (including multi-artist concerts) appear; weaker-matched artists' concerts rank below stronger matches; empty `pinnedArtists` yields title matches only.
- Dedupe: a concert matched by several pinned artists and/or by title appears once.
- Pagination: ties produce no duplicates or gaps; concert page 2+ never invokes artist matching (assert via query count or spy); pinned artists unchanged across pages; depth cap enforced per list; tampered, wrong-version, or wrong-`t` cursor returns 400.
- `EXPLAIN (ANALYZE, BUFFERS)` shows `idx_search_term_trgm` and `idx_concert_artist_artist` used on a table sized to expected data.

## 10. Maintenance and future

- Regenerate `search_term` rows in the same transaction as entity writes. Delete and re-insert variants on update.
- Changing `norm()` requires a full rebuild of `search_term`. Version the function.
- Caching: not implemented. If latency or DB load becomes a problem, measure first (p50/p95/p99 and hit-ratio simulation from real query logs), then consider caching the artist match and first-page response with short TTLs. Never cache cursor pages.
- Upgrade trigger: replace the Postgres implementation (new `SearchService` implementation, Postgres stays source of truth) if any of: relevance complaints that tuning cannot fix, need for true morphological matching or large synonym sets, or latency problems on correctly indexed queries at large scale. Candidates: OpenSearch with Nori, Meilisearch, Typesense.