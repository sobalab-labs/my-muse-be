# Development and verification guidance

Read when implementing collection behavior, changing recovery or observability,
or preparing tests and releases. Apply checks relevant to the change.

These are project-specific expectations in addition to the AWS guidance:

- Bound outbound requests with timeouts and bounded retries. Budget work against
  the invocation deadline; persist progress so interrupted work can recover.
- Keep source adapters separate from extraction, matching, persistence, and
  message handling. Preserve evidence needed to explain a concert revision.
- Use structured logs with request, job, source, and correlation identifiers.
  Distinguish unchanged sources, extraction failures, retries, and committed
  changes. Do not log credentials or sensitive payloads.
- Supply credentials through the deployment's secret configuration; never commit
  them. Document database connectivity, outbound source access, execution-role
  permissions, concurrency, timeout, retry, and dead-letter behavior before release.
- Verify extraction with fixtures; verify transaction rollback, duplicate delivery,
  concurrent claims, and database grants against PostgreSQL where applicable.
  Confirm collector migrations preserve API read contracts.
- Smoke-test handler import and representative events from the built artifact in
  a Lambda-compatible environment, including native driver loading. Document
  reproducible build/test commands and results as tooling is introduced.
