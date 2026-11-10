# Architecture Decision Records

Format: [MADR](https://github.com/adr/madr) minimal template, plus front matter (`status`, `date`, `depends-on`) and a "Pros and Cons of the Options" table.

Each ADR answers one question and can be read on its own. Dependencies are listed in the front matter, not narrated in the text.

## Status values

| Status | Meaning |
|---|---|
| `accepted` | Decision made and in effect. |
| `proposed` | Drafted; waiting for review or for the owner's decision. Also used for ADRs whose decision is deliberately deferred (the Decision Outcome section says so). |

Decision content is not edited after acceptance. Changes are made by writing a new ADR that supersedes the old one.

## Index

| # | Question | Status | Decision | Depends on |
|---|---|---|---|---|
| [0001](0001-split-units-by-trigger-and-provider-into-three-services.md) | How is the system cut into deployable units, and who owns data? | accepted | Split by trigger and provider into units; group units into three services that own their data | — |
| [0002](0002-use-shared-database-with-single-owner-tables.md) | One shared database or one per service? | accepted | One shared database, single-owner tables, reads limited to a listed set | 0001 |
| [0003](0003-use-choreographed-saga-with-forward-recovery.md) | How do we stay consistent across systems? | accepted | Choreographed saga: short local transactions, forward recovery only, no 2PC | 0002 |
| [0004](0004-transactional-outbox-and-publication-relay.md) | How do we publish reliably after a commit? | proposed | Not decided; options compared | 0002, 0003 |
| [0005](0005-use-aws-lambda-with-one-function-per-unit.md) | Where do the units run? | accepted | AWS Lambda, one function per unit | 0001 |
| [0006](0006-use-sqs-standard-with-queue-and-dlq-per-workload.md) | Which message transport, and how many queues? | proposed | SQS Standard, one queue and dead-letter queue per workload | 0005 |
| [0007](0007-use-current-state-rows-with-append-only-revisions-and-evidence.md) | How do we persist concert identity and history? | proposed | Current row + append-only revisions + evidence per revision | 0002 |
| [0008](0008-idempotency.md) | How do consumers stay idempotent? | proposed | Not decided; options compared | 0002, 0006 |
| [0009](0009-work-ownership-and-fencing.md) | How is exclusive work claimed and fenced? | proposed | Not decided; options compared | 0002, 0003 |
| [0010](0010-retry-ownership.md) | Who retries: broker or application? | proposed | Not decided; options compared | 0006 |
| [0011](0011-delivery-state-machine-and-uncertain-outcomes.md) | How are uncertain provider outcomes handled? | proposed | Deferred until providers are integrated | 0008 |
| [0012](0012-recipient-planning-snapshot-vs-send-time-evaluation.md) | Snapshot recipients at planning or evaluate at send time? | proposed | Deferred; implementation starts with the snapshot | 0002 |
| [0013](0013-regional-deployment-and-durable-recovery.md) | Where does it run, and how do we recover? | proposed | Deferred until production | 0002 |
| [0014](0014-ordering-guarantees.md) | Which ordering guarantees do we need? | proposed | Deferred; ordering is not needed now | 0006 |

Deferred ADRs (0011–0014) are numbered last on purpose.

Not written as ADRs: the choice of database product. Folded in rather than written separately: queue topology (into 0006) and collection scheduling (into 0001's unit list and 0004). Message contracts, PII handling, and alert eligibility stay in the architecture document and domain specification.
