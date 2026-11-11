import asyncio
import logging
from typing import Any

from my_muse_collector.application.protocols import ConcertIngestionClient, SourceRepository
from my_muse_collector.domain.models import ConcertCandidate
from my_muse_collector.infrastructure.observability import configure_logging
from my_muse_collector.scraping.crawler import crawl
from my_muse_collector.settings import settings

logger = logging.getLogger(__name__)


async def run_collection(
    source_repository: SourceRepository,
    ingestion_client: ConcertIngestionClient,
    *,
    source_limit: int | None = None,
    crawl_timeout_seconds: int | None = None,
) -> dict[str, Any]:
    configure_logging()
    source_limit = source_limit or settings.source_limit
    crawl_timeout_seconds = crawl_timeout_seconds or settings.crawl_timeout_seconds
    sources = await source_repository.list_enabled_sources(source_limit)
    logger.info("collection_started", extra={"source_count": len(sources)})
    crawl_result = await asyncio.to_thread(
        crawl,
        [source.model_dump(mode="json") for source in sources],
        crawl_timeout_seconds,
    )
    candidates = [ConcertCandidate.model_validate(item) for item in crawl_result["candidates"]]
    outcomes: dict[str, int] = {"received": 0}
    for offset in range(0, len(candidates), settings.ingestion_batch_size):
        batch = candidates[offset : offset + settings.ingestion_batch_size]
        batch_outcomes = await ingestion_client.submit(batch)
        if offset == 0:
            outcomes = {}
        for key, value in batch_outcomes.items():
            outcomes[key] = outcomes.get(key, 0) + value
    source_outcomes = crawl_result["source_outcomes"]
    logger.info(
        "collection_completed",
        extra={
            "source_count": len(sources),
            "candidate_count": len(candidates),
            "failed_source_count": sum(
                outcome["status"] in {"fetch_failed", "extraction_failed"}
                for outcome in source_outcomes.values()
            ),
        },
    )
    return {
        "sources": len(sources),
        "candidates": len(candidates),
        "source_outcomes": source_outcomes,
        "ingestion": outcomes,
    }
