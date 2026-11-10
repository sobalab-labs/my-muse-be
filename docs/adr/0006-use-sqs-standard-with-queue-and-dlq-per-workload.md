---
status: accepted
date: 2026-11-07
depends-on: [0005]
---
# Use Amazon SQS Standard with one queue and dead-letter queue per workload

## Context and Problem Statement

Background work is handed off through durable queues with at-least-once delivery, competing consumers, processing leases, redelivery, and a dead-letter queue per work queue. Consumers are Lambda functions, and the budget is close to zero.

Which transport carries the messages, and how are queues divided?

## Considered Options

Transport:

* Amazon SQS Standard
* Amazon SQS FIFO
* SNS + SQS fan-out
* Database table used as the queue
* Broker product (Amazon MQ/RabbitMQ, MSK/Kafka)

Topology:

* One queue per workload, each with its own dead-letter queue (`collection`, `concert-alerts`, `delivery-email`, `delivery-kakao`)
* A single queue with message-type routing
* A queue per source or per channel instance

## Decision Outcome

Chosen option: "SQS Standard, one queue per workload with a dead-letter queue each". It has a permanent free tier, integrates natively with Lambda, and provides leases (visibility timeout) and redrive out of the box. Its at-least-once, unordered delivery matches idempotent consumers, and ordering is not currently needed. One queue per workload keeps email and Kakao failures and backpressure independent.

Settings to settle during implementation: visibility timeout of at least six times the function timeout (AWS guidance), partial batch responses, a maximum receive count per queue, and alarms on dead-letter queue depth.

### Consequences

* Good, because it costs nothing at this volume (verify the free-tier limits) and adds no servers.
* Bad, because delivery is at-least-once and unordered, so consumers must be idempotent.
* Bad, because messages are limited to 256 KiB and delay to 15 minutes: payloads carry references only, and longer retry backoff must come from the application.
* Bad, because a Standard queue cannot be converted to FIFO; needing order later means new queues and a migration.
* Bad, because someone must watch dead-letter queues and redrive messages manually.

## Pros and Cons of the Options

| Option | Good | Bad |
|---|---|---|
| Transport: SQS Standard | Free tier; native Lambda integration; built-in leases and dead-lettering | No ordering; duplicates possible |
| Transport: SQS FIFO | Ordering per message group; 5-minute deduplication | Less concurrency per group; the dedup window is too short to replace idempotency; ordering not needed now |
| Transport: SNS + SQS fan-out | One event can reach several consumers | Each message has exactly one consumer; the extra layer adds nothing |
| Transport: database table as queue | No extra service; no dual write | Leases, redelivery, dead-lettering, and backpressure must be built; no native trigger on table rows |
| Transport: broker product | Rich routing and throughput | Fixed cost and operations far beyond the scale |
| Topology: queue per workload | Failures and backpressure isolated per workload and channel | More queues to configure and monitor |
| Topology: single queue | Least infrastructure | One stuck message type slows everything; dead-letter queues mix unrelated failures |
| Topology: per source or channel instance | Finest isolation | Over-engineered for <1,000 users |
