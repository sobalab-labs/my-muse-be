# My Muse Collector

Python 3.12+ concert source crawler. The package provides validated source and
candidate models, bounded Scrapy execution, JSON-LD and configured CSS selector
extractors, and application interfaces for source loading and concert ingestion.

## Development

```sh
uv sync --dev
uv run pytest
uv run ruff check .
uv run mypy src
```

The `collector` console command and `my_muse_collector.handler.handler` are the
local and Lambda entry points. They currently stop with a clear configuration
error because the repository does not define an authoritative source repository
or concert ingestion API contract. Implement those adapters after the backend
contract and ownership decision are available. The crawling and extraction
modules can be composed through `run_collection` with implementations of the
interfaces in `application/protocols.py`.

## Source contract

`WebsiteSource` accepts `id`, `artist_id`, `url`, `strategy_type`, `config`, and
optional `enabled`. The reviewed strategy keys are `json_ld` and `css_selector`.
CSS strategy configuration takes a `selectors` object with `event_container`,
`title`, and `date`, and optional `venue`, `location`, `ticket_url`,
`announcement_url`, and `external_event_id` selectors. Optional `start_urls`
selects one or more pages to fetch for a source, defaulting to its source URL. A `timezone` is required
for dates without an explicit offset. Optional `date_formats` support known
non-ISO source formats. Unknown fields and unknown strategy names fail closed.

Candidate start times must be timezone-aware. Relative ticket and announcement
links are resolved against the fetched page. Concert identity and deduplication
remain the backend's responsibility.

## Runtime behavior

The crawler uses Scrapy with robots.txt enforcement, a 20-second download
timeout, two retries, global concurrency 32, per-domain concurrency 4, and
AutoThrottle by default. These limits and source/candidate caps are configurable
with `COLLECTOR_` environment variables and validated at startup. A crawl runs in a child process so a fresh Twisted reactor is
created per invocation. The default work budget is 840 seconds to leave time
for Lambda shutdown; the source count is bounded by application settings.
Deployment still needs to set the Lambda timeout/memory/concurrency and supply
the approved backend adapters and credentials through runtime secret
configuration.

Structured logs include source identifiers, HTTP status, candidate counts, and
failure categories; page contents, validation values, and request URLs are not
logged. Per-source fetch/extraction outcomes are returned by the crawl. This
initial implementation does not yet distinguish unchanged content or retain
revision evidence because the persistence contract is unresolved. Extraction
metadata records the strategy used, not page text or full response evidence.

## Architecture gap

The collector plan describes sending candidates to the Spring Boot API, but
accepted ADRs 0001/0002 assign concert persistence and collection jobs to the
Collector service using its own schema and role. The current data model does not
define scrape-source or ingestion contracts. This implementation therefore
does not guess an HTTP endpoint, source query, database schema, migration, or
persisted evidence shape. Resolve that contract before wiring a deployable
scheduled invocation.
