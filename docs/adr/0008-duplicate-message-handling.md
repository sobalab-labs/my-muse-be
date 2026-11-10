---
status: proposed
date: 2026-11-07
depends-on: [0002, 0006]
---
# How the consumers of the four queues handle duplicate messages

## Context and Problem Statement

The queues are Amazon SQS Standard queues, which deliver at-least-once. A consumer can therefore receive the same message more than once, for example when:

* the function does its work and then crashes or times out before the message is deleted, so the queue delivers it again;
* SQS delivers the same message twice, which happens occasionally on Standard queues;
* a publisher sends the same command twice, for example after a retry;
* someone redrives a message from a dead-letter queue.

The four queues, and what a duplicate would cause:

| Queue | Consumer | One message asks for | Work identity | A duplicate would cause |
|---|---|---|---|---|
| `collection` | collector | Check one source for one scheduled occurrence | source id + scheduled occurrence | The source checked twice; duplicate revisions or alerts |
| `concert-alerts` | planner | Plan notifications for one alert | alert event id | Duplicate notification intents |
| `delivery-email` | email-sender | Send the email for one intent | intent id + dispatch version | The same email sent twice |
| `delivery-kakao` | kakao-sender | Send the Kakao message for one intent | intent id + dispatch version | The same Kakao message sent twice |

The collector's and the planner's effects are database writes, which the database itself can check. The senders' effect is an external call to the email or Kakao provider, which the database can neither undo nor deduplicate.

How does each consumer make a repeated message harmless?

## Considered Options

* Business-key unique constraints + insert-if-absent (for example, one intent per concert, user, and channel)
* Processed-message ledger: record each work identity in the same transaction as the effects, and skip it if already recorded
* Status check on the job or intent row: move `Pending → Claimed` only if it is still `Pending`
* Idempotency library or service (needs its own store)
* Queue-level deduplication (SQS FIFO)

## Decision Outcome

_Not decided yet._ The owner will study idempotency first and then decide.

### Consequences

_To be filled in when the decision is made._

## Pros and Cons of the Options

| Option | Good | Bad | Fits |
|---|---|---|---|
| Unique constraints + insert-if-absent | The database enforces it; the keys already exist in the alert policy | Only for effects that are database writes with a natural key | Planner (intents), collector (revisions, alerts) |
| Processed-message ledger | Generic; works for any handler whose effects are database writes | An extra table to clean up; the ledger write must share the effects' transaction | Collector, planner |
| Status check on job or intent row | Matches the intent states already planned (Pending, Claimed, Accepted, …) | Every transition needs careful conditional-update design; does not by itself cover a crash between the external send and the status update (see the ADR on uncertain provider outcomes) | Senders, collector |
| Idempotency library or service | Ready-made | Usually needs a separate store, which adds a second system to a one-database design | Any |
| FIFO deduplication | Built in | Five-minute window; does not cover redelivery after a crash; does not apply to Standard queues | None |
