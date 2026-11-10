---
status: accepted
date: 2026-11-07
depends-on: [0002]
---
# Use a choreographed saga with forward recovery, without distributed commits

## Context and Problem Statement

The main flow (scheduler → collector → planner → senders) changes the database and also sends messages to queues and to email/Kakao providers. The database, the queues, and the providers cannot commit together. Here, "distributed transaction" means one operation that must change the database **and** something else.

How do we keep each such operation consistent?

## Considered Options

* Choreographed saga: short local transactions linked by messages, with forward recovery only
* Orchestrated saga with a workflow engine
* Long transactions that cover a whole job
* Two-phase commit (2PC/XA)

## Decision Outcome

Chosen option: "Choreographed saga with forward recovery". It needs no extra service, works with queues and providers that cannot join a shared commit, and keeps transactions short.

Rules:

1. Only the database commits all-or-nothing. No 2PC/XA.
2. One unit of work (a request, a queue message, a scheduled run) saves all its related data in one short transaction.
3. No network calls inside a transaction. To take ownership of work, commit a short claim first, do the slow work, then save the result in another short transaction.
4. Each step reacts to the message left by the previous step, with no central coordinator. A failed step is retried or moved to a dead-letter queue, never undone: a sent email cannot be unsent.
5. Every point where one operation changes the database and something else needs a mechanism. The mechanisms are decided in their own ADRs.

### Points that need a mechanism (snapshot)

Snapshot as of 2026-11-07, for explanation only. A new point is added here and gets a mechanism.

| # | Where | What must change together | Mechanism needed |
|---|---|---|---|
| D1 | Scheduler → collection command | Save due job (DB) + send command (queue) | Reliable publishing |
| D2 | Collector → concert alert | Save revision, evidence, alert (DB) + send alert (queue) | Reliable publishing |
| D3 | Planner → delivery commands | Save intents (DB) + send commands (queues) | Reliable publishing |
| D4 | Any message sender | Send message (queue) + record that it was sent (DB) | Idempotent handling |
| D5 | Every queue consumer | Save result (DB) + delete message (queue) | Idempotent handling |
| D6 | Email / Kakao sender | Call provider (external) + save outcome (DB) | Claim before sending, idempotent handling, uncertain outcomes |
| D7 | Scheduler → collector → planner → senders | The whole chain | The saga itself |

Not distributed: the API server only reads and writes the database; the planner reads follows and preferences from the same database; fetching a source page is read-only.

### Consequences

* Good, because transactions stay short and simple, and the table names every place that needs special handling.
* Bad, because the system is eventually consistent: other readers can see a flow half-finished.
* Bad, because duplicates are normal, and without compensation a failure ends in retry, the dead-letter queue, or manual redrive.
* Bad, because no single place shows the whole flow; trace it with a correlation id and structured logs.
* Bad, because every row in the table needs its own implementation and tests.

## Pros and Cons of the Options

| Option | Good | Bad |
|---|---|---|
| Choreographed saga, forward recovery | No extra service; fits queues, serverless functions, and providers outside the commit | Flow is spread across steps and hard to see in one place |
| Orchestrated saga | Flow lives in one place and is easy to follow | Adds a service and cost; the flow is a simple forward chain that needs no central control |
| Long transactions | Looks simpler on paper | Holds connections and locks during slow work; breaks under function timeouts and pooling |
| Two-phase commit | All-or-nothing across systems | Queues and providers do not support it; a stuck coordinator can block everything |
