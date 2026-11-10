---
status: accepted
date: 2026-11-07
depends-on: [0002]
---
# Use mutable current-state concert rows with append-only revisions and per-revision evidence

## Context and Problem Statement

Repeated source checks must recognize the same concert, record changes (postponement, venue or participant change, cancellation) as history, and keep evidence of where each fact came from. Alert deduplication for major changes needs a revision identifier.

The matching rules (artists, venue, and date/time must agree) belong in the domain specification. This ADR covers only the persistence strategy for identity, revisions, and evidence. Table and column design belongs in data-model.md.

## Considered Options

* Mutable current-state row + append-only revision table + evidence per revision
* Single versioned table (each change is a new row with `valid_from` / `valid_to`)
* Event sourcing (immutable event log, state rebuilt by replay)
* Current-state row only, with a generic audit log

## Decision Outcome

Chosen option: "Mutable current-state row + append-only revision table + evidence per revision". Current-state reads stay plain queries, which suits a read-heavy API, while full history is preserved for alerts and audits.

* **Identity:** a concert has a stable surrogate id for its lifetime. Identity never depends on mutable attributes, so a postponement updates the same concert.
* **Source references:** a record maps (source, external reference) to a concert, unique per source, so re-detecting an announcement is idempotent and new announcements can attach to an existing concert.
* **Revisions:** append-only, with a per-concert sequence number, change type, status, and a snapshot of the changed attributes including the participant set. The current row points to its current revision.
* **Cancellation** is a status, never a delete.
* **Evidence:** per revision, the source, URL, identifier, size-capped extracted text, and fetch time. No full-page or media archives.
* The current row, revision, evidence, and alert events are written in one transaction.

### Consequences

* Good, because current-state reads are simple, the revision id gives major-change alerts a precise deduplication key, and repeated collection is safe to re-run.
* Bad, because the current row and the revision are two representations that must stay consistent.
* Bad, because data-model.md needs new entities (source reference, revision, evidence), and it must be decided whether the official announcement URL stays a column or is derived from evidence.
* Bad, because evidence text grows storage; a retention policy is needed.

## Pros and Cons of the Options

| Option | Good | Bad |
|---|---|---|
| Current row + revisions + evidence | Simple reads with full history | Two representations to keep consistent |
| Single versioned table | One source of truth; point-in-time queries are natural | Every read filters for the current row; foreign keys to a changing row are awkward |
| Event sourcing | History is complete by construction | Replay and projections are out of proportion to the scale and the read-heavy workload |
| Current row + generic audit log | Least work | History is not domain-aware: alerts cannot reference a revision, and evidence has no home |
