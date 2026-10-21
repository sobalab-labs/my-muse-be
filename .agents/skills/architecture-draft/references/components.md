# Generic Component Vocabulary and Pattern Hints

Use ONLY these generic names in the document. Never substitute a product, framework,
or vendor name. Describe needed characteristics instead (e.g. "database that supports
transactions across order and payment records").

## Component types

| Component | Role | Include when |
|---|---|---|
| Client | Web/mobile/device/partner system calling the service | Always (name the kinds) |
| Entry point (gateway / load balancer) | Routing, auth check, rate limiting, spreading load | More than one service instance, or external exposure |
| API service | Handles synchronous requests, business logic | Always |
| Background worker | Runs slow or retryable work outside the request | Slow, bursty, or retry-heavy tasks exist |
| Message queue | Buffers work between producers and workers | Work must survive spikes or failures |
| Event stream | Ordered, replayable log of events for several consumers | Many independent consumers need the same events |
| Database | Durable structured data (state which characteristics: transactions, flexible schema, key-based access, time-ordered) | Always, unless data is only files |
| Cache | Fast copy of hot data | Read-heavy and repeated reads of the same data |
| Object storage | Large files/media | Binary content exists |
| Content delivery layer | Serves static or media content near users | Large static content, geographically spread users |
| Search index | Text or complex filtering queries | Queries a database can't serve well |
| Scheduler | Time-based jobs | Periodic work exists |
| Identity component | Authentication and authorization | Users or partners must be identified |
| Observability | Logs, metrics, traces, alerts | Always (one line) |
| Analytics store | Reporting queries separated from live traffic | Heavy reporting would disturb live traffic |

## Pattern hints (constraint -> structural response)

| Signal from inputs | Structural response to consider |
|---|---|
| Small load, small team | One deployable unit with clear internal modules; no separate services yet |
| Read-heavy, repeated reads | Cache in front of the database; read copies of the database |
| Spiky or bursty writes | Queue between entry point and workers; accept-then-process |
| Slow tasks (media, reports, notifications) | Move to background workers; return immediately with a status |
| Must not double-apply or lose an operation | Idempotent operations; durable write before acknowledging; transactional boundary around the critical data |
| Near-zero downtime | Multiple instances of every stateless component; redundancy for stateful ones; no single point of failure |
| Users in several regions | Content delivery layer; consider regional placement later (flag as open question) |
| Large media | Object storage + content delivery layer; keep database for metadata only |
| Many consumers of the same events | Event stream instead of a plain queue |
| Different parts scale very differently | Candidate to split into a separate service (mark as "later, if needed") |
| Compliance / residency | Note which data must stay where; flag for detailed design |

## Rules

- Each component added must cite the input that justifies it. No cited input, no component.
- Prefer fewer components. If the load label is "Small", the default is one API
  service, one database, and little else.
- Splitting into multiple services needs a concrete reason (different scaling,
  different failure isolation, different owners). Otherwise list it as a future option.
