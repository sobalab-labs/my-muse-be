# Shared PostgreSQL and transaction guidance

Read when changing persistence, migrations, jobs, or messaging.

The collector uses the same PostgreSQL database as the Spring API server and
notifier. Shared storage preserves service ownership:

- Use the collector's own database role and `collector` schema. Runtime write
  privileges must cover only collector-owned tables; cross-service access must
  follow explicit read-only grants or views from [ADR-0002](../../../../docs/adr/0002-use-shared-database-with-single-owner-tables.md).
- Collector ownership includes concerts, concert history, evidence, source
  references, alerts, and collection jobs as defined by the authoritative model.
  Proposed entities must be designed before migrations are added.
- Read the API-owned artist catalog for matching through documented contracts.
  Do not mutate artists, user data, follows, likes, preferences, or notifier data.
  Artist creation requires an explicit ownership decision first.
- Coordinate migrations through the shared migration pipeline. Use
  expand/contract for structures consumed by other services; avoid runtime DDL
  and destructive schema changes that break the Spring API's queries.
- Set a small per-service connection limit and budget total connections against
  Lambda concurrency and API usage. Reused connections must recover from stale
  sessions and leave no open transaction between invocations.

Use short PostgreSQL transactions at READ COMMITTED. Claim work and commit before
fetching; perform slow extraction outside the transaction, then commit the related
result atomically. Never fetch sources or publish messages while holding a
transaction. Use constraints, conflict handling, and conditional claims to enforce
correctness under concurrency.

Commit related concert changes, revisions, evidence, alerts, and outbox records
together. Scheduled jobs and their collection commands also need atomic outbox
writes. Assume duplicate delivery and retries: durable identifiers and database
constraints must prevent duplicate jobs, revisions, and alerts. Relay publication
can duplicate messages; select its implementation only after resolving [ADR-0004](../../../../docs/adr/0004-transactional-outbox-and-publication-relay.md).
