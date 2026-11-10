---
status: proposed
date: 2026-11-07
depends-on: [0002, 0003]
---
# How to publish messages reliably after a database commit

## Context and Problem Statement

Several steps change database state and must also send a message to a queue: the scheduler commits a job and triggers the collector, the collector commits alerts and triggers the planner, the planner commits intents and triggers the senders. The database and the queue have no shared transaction (the "dual-write problem"). A crash between the two steps leaves state committed but no message sent, or a message sent for state that was rolled back.

The system must not lose committed data, and durable work must resume after recovery. Compute is short-lived functions, so there is no always-on publisher process.

## Considered Options

* Transactional outbox + polling relay (scheduled function)
* Transactional outbox + immediate dispatch after commit, with a sweeper for leftovers
* Direct publish after commit + reconciliation sweep (no outbox table)
* Publish before commit, or inside the transaction
* Change data capture (a publisher reading the database change stream)
* Database table as the queue (no external broker)

## Decision Outcome

_Not decided yet._ The owner will decide after studying the outbox pattern.

### Consequences

_To be filled in when the decision is made._

## Pros and Cons of the Options

| Option | Good | Bad |
|---|---|---|
| Outbox + polling relay | The message commits atomically with the state, so nothing is lost; the relay can retry safely | Delay equals the polling interval (a managed timer is typically one minute at best); an outbox table and a relay function; duplicates are possible, so consumers must be idempotent; frequent polling can keep a scale-to-zero database awake |
| Outbox + immediate dispatch + sweeper | Fast in the common case; the sweeper covers crashes | Two publishing paths to build and reason about |
| Direct publish + reconciliation sweep | Simplest; no outbox table | A crash after commit loses the message until a sweep finds it, and the sweep needs a "published" marker on each state row, which is a hidden outbox |
| Publish before commit or inside the transaction | No outbox | Messages can describe state that never committed, and a failure in either step leaves the other behind. Listed for completeness |
| Change data capture | Applications do not publish at all | Needs a long-running consumer of the change stream, which does not fit short-lived functions or the budget |
| Database table as the queue | No dual write at all | Leases, redelivery, dead-lettering, and backpressure must be built, and there is no native trigger on table rows |
