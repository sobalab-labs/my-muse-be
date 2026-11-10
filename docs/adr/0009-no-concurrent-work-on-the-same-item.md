---
status: proposed
date: 2026-11-07
depends-on: [0002, 0003, 0005]
---
# How concurrent function instances avoid working on the same item

## Context and Problem Statement

On Lambda, one function does not mean one running copy. As messages arrive, the platform runs several instances of the same function at the same time: several `collector` invocations, several `email-sender` invocations, and so on. Normally each instance gets a different message. But two instances can end up with the same item:

* **Collector:** two `collection` messages for the same source arrive close together (the timer fires twice, or the next scheduled check is sent while the previous one is still running), or the same message is delivered twice at once. Two checks of one source must not overlap.
* **Senders:** the same `delivery-email` or `delivery-kakao` message is delivered twice at once, so two instances start sending the same notification.

The slow part (fetching a source, calling a provider) runs outside any database transaction, so a transaction lock cannot cover it. An instance can also crash halfway, so a claim must not stay "in progress" forever.

How does an instance claim one source check or one notification so that no other instance works on it at the same time?

## Considered Options

* Lease row: a claim stored in the database with an owner token and an expiry time; after a crash the lease expires and another instance can take over
* Row lock (`SELECT … FOR UPDATE SKIP LOCKED`) in a short transaction
* Transaction-scoped advisory lock
* Queue visibility timeout alone
* Cap the function at one concurrent invocation (reserved concurrency of 1)

## Decision Outcome

_Not decided yet._

### Consequences

_To be filled in when the decision is made._

## Pros and Cons of the Options

| Option | Good | Bad |
|---|---|---|
| Lease row with token and expiry | Survives crashes (the lease expires) and works across instances | The lease must outlast the function timeout, or writes need a token check, so an instance that lost its lease cannot overwrite the new owner's result |
| Row lock in a short transaction | Simple and safe for short claims | The lock cannot be held across the slow fetch or send |
| Transaction-scoped advisory lock | No extra columns | Ends with the transaction, so it cannot guard the slow part either; session-level locks are not safe through a transaction-mode connection pooler |
| Queue visibility timeout alone | Nothing to build | Protects only one message, not two different messages for the same source; works only if the timeout is longer than the function timeout |
| Cap at one concurrent invocation | Nothing to build; no overlap by construction | Throughput is capped, so sources are checked one at a time; throttled messages return to the queue and can be dead-lettered by the receive limit; does not stop duplicates over time |
