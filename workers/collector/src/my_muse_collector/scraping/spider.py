import logging
from collections.abc import AsyncIterator, Iterator
from typing import Any

import scrapy
from scrapy.http import Response

from my_muse_collector.domain.models import WebsiteSource
from my_muse_collector.scraping.strategies.registry import StrategyRegistry

logger = logging.getLogger(__name__)


class ConcertSpider(scrapy.Spider):
    name = "concert_sources"

    def __init__(self, sources: list[dict[str, Any]], **kwargs: Any) -> None:
        super().__init__(**kwargs)
        self.sources = [
            WebsiteSource.model_validate(source)
            for source in sources
            if source.get("enabled", True)
        ]
        self.registry = StrategyRegistry()
        self.source_outcomes: dict[str, dict[str, Any]] = {}

    def start_requests(self) -> Iterator[scrapy.Request]:
        for source in self.sources:
            try:
                strategy = self.registry.resolve(source.strategy_type)
                strategy.validate_config(source)
            except Exception as exc:
                source_id = str(source.id)
                self.source_outcomes[source_id] = {
                    "source_id": source_id,
                    "status": "configuration_failed",
                    "error_type": type(exc).__name__,
                }
                logger.error(
                    "source_configuration_failed",
                    extra={"source_id": source_id, "error_type": type(exc).__name__},
                )
                continue
            for start_url in strategy.start_urls(source):
                yield scrapy.Request(
                    start_url,
                    callback=self.parse_source,
                    errback=self.request_failed,
                    cb_kwargs={"source": source},
                    meta={"source_id": str(source.id), "dont_redirect": False},
                )

    async def start(self) -> AsyncIterator[scrapy.Request]:
        """Use Scrapy's current async start hook while retaining 2.11 compatibility."""
        for request in self.start_requests():
            yield request

    def parse_source(self, response: Response, source: WebsiteSource) -> Iterator[dict[str, Any]]:
        try:
            strategy = self.registry.resolve(source.strategy_type)
            candidate_count = 0
            for candidate in strategy.extract(response, source):
                candidate_count += 1
                yield candidate.model_dump(mode="json")
            outcome = {
                "source_id": str(source.id),
                "status": "success",
                "http_status": response.status,
                "candidate_count": candidate_count,
            }
            self.source_outcomes[str(source.id)] = outcome
            logger.info(
                "source_extracted",
                extra={
                    "source_id": str(source.id),
                    "status": response.status,
                    "candidate_count": candidate_count,
                },
            )
        except Exception as exc:
            self.source_outcomes[str(source.id)] = {
                "source_id": str(source.id),
                "status": "extraction_failed",
                "error_type": type(exc).__name__,
            }
            logger.error(
                "source_extraction_failed",
                extra={"source_id": str(source.id), "error_type": type(exc).__name__},
            )

    def request_failed(self, failure: Any) -> None:
        request = failure.request
        source_id = request.meta.get("source_id")
        error_type = failure.type.__name__
        self.source_outcomes[source_id] = {
            "source_id": source_id,
            "status": "fetch_failed",
            "error_type": error_type,
        }
        logger.warning(
            "source_request_failed",
            extra={
                "source_id": source_id,
                "error_type": error_type,
            },
        )
