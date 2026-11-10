---
status: accepted
date: 2026-11-07
depends-on: [0001]
---
# Deploy the collector and notifier on AWS Lambda, with one function per unit

## Context and Problem Statement

The collector and notifier run background work: scheduled source checks, and notification bursts of 5–10x average load. Both are started by timers and queue messages. Minutes of downtime are acceptable, there are fewer than 1,000 users, and there is no budget for 24/7 compute.

Where do their units run?

## Considered Options

* AWS Lambda, one function per unit
* Container tasks started on demand (e.g., ECS Fargate tasks triggered by a timer or a queue)
* Always-on containers or VMs (e.g., ECS Fargate services, EC2/Lightsail)

## Decision Outcome

Chosen option: "AWS Lambda, one function per unit". Billing is per invocation, so there is no idle cost. Queues and timers trigger functions natively, and per-function concurrency limits keep source and channel failures apart and protect provider rate limits.

Each unit of the collector and notifier is one function with its own reserved concurrency and its own credentials. Units that share code can ship as one artifact with different handlers; packaging is out of scope.

### Consequences

* Good, because there is no idle cost, and scaling follows queue depth.
* Good, because a failing source or channel is contained by its own function and concurrency limit.
* Bad, because of the 15-minute maximum duration: a collection job must finish within it or be split.
* Bad, because the first message after an idle period waits for a cold start, which is acceptable for background work.
* Bad, because many concurrent functions can exhaust database connections, so pooling and reserved concurrency are required.
* Bad, because there are more functions to configure, observe, and release (infrastructure as code is effectively required), and triggers tie the system to AWS.

## Pros and Cons of the Options

| Option | Good | Bad |
|---|---|---|
| Lambda, one function per unit | Pay per use; native triggers and per-function concurrency limits | 15-minute limit, cold starts, more functions |
| On-demand container tasks | No duration limit; billed only while running | Slower start; more glue code to trigger from queues and to cap concurrency |
| Always-on containers or VMs | No cold starts or duration limit; long-lived connections are easy | A 24/7 cost the budget does not allow |