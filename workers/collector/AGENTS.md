# Collector agent index

## Always applicable

- Consult [Architecture Decision Records index](../../docs/adr/adr-index.md) to identify and review applicable architectural decisions before implementation.
- Accepted ADRs govern architecture; the data model governs entities. Proposed ADRs and diagram snapshots are not finalized contracts. Report gaps instead of inventing decisions.
- Never commit credentials or log sensitive payloads. Keep work scoped and distinguish implemented behavior from plans and unverified assumptions.
- Prefer available codebase-memory-mcp graph tools for code discovery. Use text search for literals, configuration, documentation, or insufficient graph results.

## Read only what the task needs

Before changing an area below, read its linked guide and relevant authoritative
sections. Do not preload every reference or fetch external links automatically.
Follow additional references only when needed to resolve the current task.

| Task | Read |
|---|---|
| Persistence, migrations, connections, jobs, messaging | [Database guide](docs/agent-guides/database.md); relevant records from [ADR Index](../../docs/adr/adr-index.md) |
| Publication relay design | Relevant records from [ADR Index](../../docs/adr/adr-index.md) |
| Lambda handlers, dependencies, runtime, packaging, deployment | [Lambda guide](docs/agent-guides/lambda.md) |
| Collection behavior, crawling architecture, strategies, tests | [Development guide](docs/agent-guides/development.md), [Collector plan](docs/agent-guides/my-muse-collector-plan.md) |
| Entities, matching, revisions, invariants | [Data model](../../docs/data-model/data-model.md), [invariants](../../docs/data-model/invariants.md), [transaction scenarios](../../docs/data-model/transaction-scenarios.md) |
| Service boundaries or ownership | Relevant records from [ADR Index](../../docs/adr/adr-index.md) |

This guide prepares future development; it does not assert that these components already exist. Document reproducible commands as tooling is introduced.
