# My Muse Collector --- Scraper Service Plan

## 1. Purpose

Build a Python-based collector that periodically discovers newly
announced concerts from many artist-associated website URLs and submits
normalized concert candidates to the existing Spring Boot API.

The service must support:

-   Many artists and multiple website URLs per artist.
-   Different website structures and extraction methods.
-   Concurrent scraping with per-domain limits.
-   Configurable strategies for websites that share a structure.
-   Custom adapters for websites that cannot be handled by generic
    strategies.
-   Reliable retries, failure isolation, observability, and idempotent
    ingestion.
-   Scheduled execution through AWS EventBridge Scheduler.

The collector owns external data acquisition and extraction. The Spring
Boot API remains the authority for concert persistence and
business-level deduplication.

## 2. Recommended technology

  -----------------------------------------------------------------------
  Concern                             Choice
  ----------------------------------- -----------------------------------
  Language                            Python 3.12+

  Project/dependency management       `uv`

  Crawling engine                     Scrapy

  HTML parsing                        Scrapy selectors (Parsel / lxml);
                                      use BeautifulSoup only when useful

  Data validation                     Pydantic

  Configuration                       Database-backed JSON configuration,
                                      validated with Pydantic; TOML for
                                      local defaults if needed

  Browser automation                  Playwright, only for
                                      JavaScript-rendered sites that
                                      require it

  LLM extraction                      Optional fallback behind an
                                      interface; not the default
                                      extraction path

  Testing                             pytest, pytest-asyncio

  Linting/formatting                  Ruff

  Type checking                       mypy or Pyright

  Logging                             Python standard logging with
                                      structured JSON output

  Runtime                             AWS Lambda

  Scheduling                          Amazon EventBridge Scheduler

  Deployment                          ZIP package initially; container
                                      image if browser/OS dependencies
                                      make ZIP impractical

  Infrastructure as code              AWS CDK (TypeScript)

  Concert persistence                 Existing Spring Boot ingestion API
  -----------------------------------------------------------------------

Scrapy is recommended because it provides the crawling infrastructure
this workload needs: asynchronous request scheduling, concurrency
controls, per-domain limits, retry middleware, download timeouts,
throttling, and item pipelines. It does not remove the need to implement
website-specific extraction logic.

## 3. High-level architecture

``` mermaid
flowchart TD
    A[EventBridge Scheduler] --> B[Collector Lambda Handler]
    B --> C[Application Orchestrator]
    C --> D[Load Enabled Scrape Sources]
    D --> E[Create Scrapy Crawl Jobs]
    E --> F[Scrapy Engine and Scheduler]
    F --> G[Downloader Middleware]
    G --> H[Website Responses]
    H --> I[Strategy Resolver]
    I --> J[JSON-LD Strategy]
    I --> K[CSS/XPath Config Strategy]
    I --> L[Platform / Custom Adapter]
    I --> M[Browser Strategy when required]
    J --> N[Concert Candidate]
    K --> N
    L --> N
    M --> N
    N --> O[Normalize and Validate with Pydantic]
    O --> P[Batch Concert Ingestion Client]
    P --> Q[Spring Boot API]
    Q --> R[Deduplication and Persistence]
```

### Responsibility boundaries

-   **EventBridge Scheduler:** Invokes the Lambda on a recurring
    schedule. Scheduling logic does not live inside the scraper.
-   **Lambda handler:** Adapts the AWS event/context to the application
    entry point. Keep it thin.
-   **Application orchestrator:** Loads scrape sources, starts crawl
    work, coordinates results, and reports the run outcome.
-   **Scrapy infrastructure:** Owns request scheduling, downloads,
    concurrency, retries, and response delivery.
-   **Strategy resolver:** Selects the extraction strategy for each
    source.
-   **Extraction strategies:** Convert a website response into common
    concert candidates.
-   **Normalizer/validator:** Converts extracted values into a canonical
    representation and validates them.
-   **Spring Boot API:** Owns concert identity rules, deduplication, and
    persistence.

## 4. Data model and source configuration

Keep artist identity and website-source configuration separate. An
artist may have many links, and a link may have a scrape configuration.

A possible conceptual model:

``` text
Artist
  id
  name
  ...

ArtistLink
  id
  artist_id
  platform
  url

ScrapeSource
  id
  artist_link_id
  strategy_type
  config              JSON / JSONB
  enabled
  last_scraped_at
  last_success_at
  last_status
  last_error
```

`ScrapeSource` represents a URL that the collector knows how to inspect.
Do not assume every artist link is automatically scrapeable; links may
be social profiles, ticket pages, or unrelated pages.

### Example source configuration

``` json
{
  "strategy_type": "css_selector",
  "config": {
    "start_urls": ["https://example.com/events"],
    "selectors": {
      "event_container": ".event-item",
      "title": ".event-title",
      "date": ".event-date",
      "venue": ".venue-name",
      "ticket_url": "a.ticket-link::attr(href)"
    }
  }
}
```

Validate configuration with Pydantic before starting a crawl. Store
secrets outside this configuration, using AWS Secrets Manager or an
appropriate runtime secret mechanism.

### Configuration principle

-   Store **which URL to inspect**, **which strategy to use**, and
    **data-only selector/configuration values** in the database.
-   Keep executable extraction behavior in reviewed Python code.
-   Adding a new source that fits an existing generic strategy should
    require only a database/configuration change.
-   A genuinely new or unusual website may require a new reusable
    adapter or custom strategy. Do not attempt to represent arbitrary
    website behavior entirely as configuration.

TOML can be useful for local development defaults or checked-in
examples. For per-source configuration managed at runtime, database
JSON/JSONB is generally more convenient than maintaining a separate TOML
file for every artist website.

## 5. Extraction strategy design

Use a Strategy Pattern so each source can select an extraction
implementation without coupling the orchestrator to website details.

A conceptual interface:

``` python
from typing import Protocol

class ExtractionStrategy(Protocol):
    def extract(self, response) -> list["ConcertCandidate"]:
        ...
```

The concrete method may be synchronous because parsing an
already-downloaded response is generally CPU-local. If a strategy
performs additional I/O, isolate that work and integrate it with
Scrapy's asynchronous request flow rather than blocking the reactor.

### Recommended strategy priority

1.  **JSON-LD strategy**
    -   Inspect `application/ld+json` data, especially schema.org
        `Event` objects.
    -   Prefer structured data when it is present and trustworthy.
2.  **CSS/XPath configuration strategy**
    -   Use database-provided selectors for websites with stable,
        conventional markup.
    -   Support configured event containers and field selectors.
    -   Normalize relative links and parse date strings carefully.
3.  **Reusable platform/site adapters**
    -   Implement a shared adapter when multiple artists use the same
        ticketing platform, CMS, or website structure.
    -   Prefer platform-level reuse over artist-specific classes.
4.  **Custom Python strategies**
    -   Use only when a site's navigation, markup, pagination, or
        extraction rules cannot be expressed cleanly through generic
        configuration.
    -   Keep each adapter small and focused.
5.  **Browser-assisted strategy**
    -   Use Playwright only when the required content is rendered
        client-side or cannot be obtained through ordinary HTTP
        requests.
    -   Avoid launching a browser for every source. Browser work is
        resource-intensive and should have bounded concurrency.
6.  **LLM-assisted extraction**
    -   Treat as an optional fallback for ambiguous or irregular
        content, not the default.
    -   Bound concurrency and cost; validate all output; retain source
        evidence such as the originating URL and relevant text.
    -   Do not let an LLM invent missing dates, venues, or ticket URLs.

### Strategy resolution

``` mermaid
flowchart TD
    A[ScrapeSource] --> B[Read strategy_type and config]
    B --> C{Registered strategy?}
    C -->|json_ld| D[JSON-LD strategy]
    C -->|css_selector| E[CSS/XPath config strategy]
    C -->|platform adapter| F[Reusable adapter]
    C -->|custom strategy| G[Custom Python strategy]
    C -->|browser| H[Browser-assisted strategy]
    D --> I[ConcertCandidate list]
    E --> I
    F --> I
    G --> I
    H --> I
    I --> J[Normalize and validate]
```

The resolver should use an explicit registry or factory. Avoid dynamic
imports based on arbitrary database strings. Strategy identifiers should
map to known, reviewed implementations.

## 6. Scrapy execution model

Use Scrapy as the crawling engine, not as the domain model or the whole
application architecture.

-   Load enabled scrape sources from the Spring API or the approved
    data-access mechanism.
-   Create crawl requests with source identifiers and strategy metadata
    in request metadata.
-   Use Scrapy's scheduler and downloader to manage concurrent HTTP
    work.
-   Apply global and per-domain concurrency limits.
-   Configure request timeouts, retries, and sensible download delays.
-   Respect website terms, robots policies where applicable, and rate
    limits.
-   Use AutoThrottle where it is useful, while retaining explicit
    concurrency ceilings.
-   Isolate failures by source: one broken website should not abort the
    entire daily run.
-   Record source-level outcomes and enough context to diagnose
    failures.
-   Avoid unbounded request creation or retaining every response/result
    in memory for a large crawl.

Illustrative starting settings (tune using measurements and website
policies):

``` python
CONCURRENT_REQUESTS = 32
CONCURRENT_REQUESTS_PER_DOMAIN = 4
DOWNLOAD_TIMEOUT = 20
RETRY_ENABLED = True
RETRY_TIMES = 2
AUTOTHROTTLE_ENABLED = True
```

These are starting values, not universal optimal settings. The
appropriate limits depend on the number of sources, website policies,
Lambda memory/timeout, response sizes, and observed latency.

### Scrapy and custom strategies

Pass a source identifier and strategy metadata through each Scrapy
request. When a response arrives, resolve the configured strategy and
extract candidates. Keep Scrapy `Request` and `Response` objects at the
infrastructure boundary where practical; expose normalized domain models
to the application layer.

If a strategy needs to follow pagination or request additional pages, it
should be able to yield or schedule further Scrapy requests through a
controlled adapter. Do not hide arbitrary network access inside a
parser.

## 7. Canonical concert candidate

All strategies should return the same normalized model, independent of
the source website.

Example fields:

``` text
ConcertCandidate
  title
  start_datetime
  venue_name              optional
  location_text           optional
  ticketing_url           optional
  announcement_url
  source_url
  artist_id
  external_event_id       optional
  extraction_metadata     optional
```

Use Pydantic to validate types and required fields. Preserve provenance
(`source_url` and `announcement_url`) so records can be traced back to
the original website.

Be explicit about date/time interpretation and timezone handling. Do not
silently assume a timezone when the source does not provide enough
information. Keep extraction metadata separate from fields that define
concert identity.

## 8. Deduplication and persistence

The collector should not independently reimplement the backend's
authoritative concert identity and persistence rules.

Recommended flow:

1.  Extract candidate concerts.
2.  Normalize and validate candidates.
3.  Group or batch candidates for ingestion.
4.  Submit them to a Spring Boot ingestion endpoint.
5.  Let Spring Boot apply the canonical deduplication rules and persist
    new concerts.
6.  Return ingestion outcomes, including newly created and already-known
    concerts.
7.  Update scrape-source status and metrics.

The ingestion endpoint should be idempotent. A repeated scheduled run,
retry, or duplicate source must not create duplicate concerts.

Define the backend's matching rules explicitly. Possible evidence
includes an external event ID, normalized announcement URL, ticket URL,
artist, title, venue, and start time. Do not rely on title alone.

## 9. AWS deployment and scheduling

### Initial deployment

Start with one scheduled Lambda invocation that processes a bounded set
of sources.

``` mermaid
flowchart TD
    A[EventBridge Scheduler] -->|Scheduled invocation| B[Collector Lambda]
    B --> C[Load scrape sources]
    C --> D[Scrapy crawl]
    D --> E[Normalize candidates]
    E --> F[Spring Boot ingestion API]
    F --> G[PostgreSQL]
```

Use a thin synchronous Lambda handler that invokes the asynchronous
application entry point:

``` python
# handler.py
import asyncio

from my_muse_collector.main import run


def handler(event, context):
    return asyncio.run(run(event, context))
```

The application should also have a local entry point so it can be run
and tested without Lambda:

``` python
# main.py
import asyncio


async def run(event=None, context=None):
    # Load sources, execute crawl, validate results,
    # submit batches, and record the run outcome.
    ...


if __name__ == "__main__":
    asyncio.run(run())
```

The exact Scrapy integration must respect Scrapy/Twisted reactor
lifecycle constraints. Do not assume that repeatedly starting and
stopping a crawler process inside one already-running asyncio loop will
work without a deliberate integration approach. Select and test one
supported integration pattern early.

### Lambda constraints

-   Lambda has a maximum execution timeout of 15 minutes.
-   Set memory and timeout based on measured workload.
-   Ensure the crawl can stop cleanly before the Lambda deadline.
-   Avoid assuming a large crawl will always fit in one invocation.
-   Configure network access to the Spring API and database
    appropriately.
-   Store credentials in AWS Secrets Manager or another suitable secret
    store.
-   Make invocations safe to retry.

### Scaling path

When the source count or crawl duration outgrows one invocation, move to
independently processed batches:

``` mermaid
flowchart TD
    A[EventBridge Scheduler] --> B[Dispatcher Lambda]
    B --> C[Load enabled sources]
    C --> D[Create bounded source batches]
    D --> E[SQS]
    E --> F[Worker Lambda]
    F --> G[Scrapy processes assigned batch]
    G --> H[Spring Boot ingestion API]
    H --> I[PostgreSQL]
    F --> J[Failure handling / DLQ]
```

Each worker should own its assigned batch. Do not assume Scrapy's
in-memory scheduler is shared between Lambda instances. Configure SQS
visibility timeout, retry behavior, dead-letter handling, and
idempotency deliberately.

## 10. Project structure

``` text
my-muse-collector/
├── pyproject.toml
├── uv.lock
├── README.md
├── Dockerfile                  # Optional initially
├── config/
│   └── scraping.toml           # Local defaults / examples only
├── src/
│   └── my_muse_collector/
│       ├── __init__.py
│       ├── handler.py          # AWS Lambda adapter
│       ├── main.py             # Local/application entry point
│       ├── settings.py
│       │
│       ├── domain/
│       │   ├── models.py       # WebsiteSource, ConcertCandidate
│       │   └── protocols.py    # Application-facing interfaces
│       │
│       ├── application/
│       │   ├── orchestrator.py
│       │   ├── source_loader.py
│       │   └── ingestion_service.py
│       │
│       ├── scraping/
│       │   ├── crawler.py      # Scrapy integration
│       │   ├── settings.py
│       │   ├── middlewares/
│       │   └── pipelines/
│       │
│       ├── strategies/
│       │   ├── base.py
│       │   ├── registry.py
│       │   ├── json_ld.py
│       │   ├── css_selector.py
│       │   ├── platform/
│       │   ├── custom/
│       │   ├── browser.py
│       │   └── llm.py
│       │
│       ├── infrastructure/
│       │   ├── backend_client.py
│       │   ├── source_repository.py
│       │   └── observability.py
│       │
│       └── schemas/
│           ├── source_config.py
│           └── concert.py
│
└── tests/
    ├── unit/
    │   ├── strategies/
    │   └── normalization/
    ├── integration/
    │   ├── scraping/
    │   └── ingestion/
    └── fixtures/
        ├── json_ld/
        └── websites/
```

Keep this structure proportional to the implementation. It is a
suggested boundary map, not a requirement to create empty modules for
every directory immediately.

## 11. Development workflow and quality

Initialize the project with `uv`:

``` bash
uv init --package my-muse-collector
cd my-muse-collector

uv add scrapy pydantic pydantic-settings httpx structlog
uv add --dev pytest pytest-cov ruff mypy
```

Add Playwright and its browser dependencies only when a real source
requires them. HTTPX may still be useful for non-crawling API calls,
such as communicating with the Spring backend; Scrapy should own the
website crawl requests.

Recommended tests:

-   Unit tests for each extraction strategy using saved HTML fixtures.
-   Configuration validation tests.
-   Date/time and URL normalization tests.
-   Integration tests against representative websites or controlled
    local fixtures.
-   Ingestion-client tests covering retries and idempotent requests.
-   A small end-to-end crawl test.
-   Tests for malformed pages, empty results, timeouts, HTTP errors, and
    partial failures.

Use saved HTML fixtures so strategy tests do not depend on live
websites.

## 12. Implementation sequence

1.  **Bootstrap:** Create the `uv` package, lint/test configuration,
    Lambda handler, and local entry point.
2.  **Contracts:** Define `WebsiteSource`, source configuration schemas,
    `ConcertCandidate`, and the Spring ingestion API contract.
3.  **First crawl:** Integrate Scrapy and fetch a small set of
    representative sources with bounded concurrency.
4.  **First strategies:** Implement JSON-LD and configurable CSS/XPath
    extraction.
5.  **Strategy registry:** Add reusable platform adapters and a
    controlled extension point for custom strategies.
6.  **Normalization:** Validate dates, URLs, required fields,
    provenance, and timezone assumptions.
7.  **Ingestion:** Add batched Spring API submission and backend-owned
    idempotent deduplication.
8.  **Reliability:** Add retries, per-source outcomes, structured logs,
    metrics, and failure isolation.
9.  **AWS deployment:** Deploy the Lambda and EventBridge Scheduler
    using CDK.
10. **Scale when measured:** Introduce SQS and multiple workers only
    when crawl duration, source count, or failure isolation justifies
    it.
11. **Special cases:** Add Playwright and LLM extraction only for
    sources that demonstrably need them.

## 13. Key architectural decisions

-   Use Scrapy for crawling infrastructure; do not confuse it with the
    website extraction strategy system.
-   Keep extraction strategies independent of persistence and, where
    practical, independent of Scrapy response objects.
-   Prefer generic structured-data and selector strategies before custom
    code.
-   Store source configuration as validated database JSON; reserve
    Python code for behavior.
-   Keep Spring Boot authoritative for concert identity, deduplication,
    and persistence.
-   Make crawl and ingestion operations observable and safe to retry.
-   Start with one bounded Lambda worker; introduce SQS-based
    distribution when measurements justify it.
-   Do not add Playwright, LLM extraction, a web framework, or a
    distributed queue preemptively.
