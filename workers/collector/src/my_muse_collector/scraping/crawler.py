from __future__ import annotations

import multiprocessing
from typing import Any, cast

from scrapy import signals
from scrapy.crawler import CrawlerProcess

from my_muse_collector.infrastructure.observability import configure_logging
from my_muse_collector.scraping.spider import ConcertSpider
from my_muse_collector.settings import Settings, settings


def _crawl_worker(sources: list[dict[str, Any]], options: dict[str, int], output: Any) -> None:
    configure_logging()
    settings = {
        "BOT_NAME": "my_muse_collector",
        "SPIDER_MODULES": ["my_muse_collector.scraping"],
        "ROBOTSTXT_OBEY": True,
        "CONCURRENT_REQUESTS": options["concurrent_requests"],
        "CONCURRENT_REQUESTS_PER_DOMAIN": options["per_domain_concurrency"],
        "DOWNLOAD_TIMEOUT": options["download_timeout_seconds"],
        "RETRY_ENABLED": True,
        "RETRY_TIMES": options["retry_times"],
        "AUTOTHROTTLE_ENABLED": True,
        "AUTOTHROTTLE_TARGET_CONCURRENCY": 2.0,
        "CLOSESPIDER_ITEMCOUNT": options["max_candidates"],
        "LOG_LEVEL": "INFO",
        "LOG_INSTALL_ROOT_HANDLER": False,
        "TELNETCONSOLE_ENABLED": False,
    }
    process = CrawlerProcess(settings=settings)
    crawler = process.create_crawler(ConcertSpider)
    items: list[dict[str, Any]] = []
    crawler.signals.connect(
        lambda item, response, spider: items.append(dict(item)), signal=signals.item_scraped
    )
    process.crawl(crawler, sources=sources)
    process.start()
    spider = crawler.spider
    output.put(
        {
            "candidates": items,
            "item_count": len(items),
            "source_outcomes": spider.source_outcomes if isinstance(spider, ConcertSpider) else {},
        }
    )


def crawl(
    sources: list[dict[str, Any]],
    timeout_seconds: int | None = None,
    options: Settings | None = None,
) -> dict[str, Any]:
    """Run Scrapy in a child process so each invocation owns a fresh Twisted reactor."""
    options = options or settings
    timeout_seconds = timeout_seconds or options.crawl_timeout_seconds
    context = multiprocessing.get_context("spawn")
    output = context.Queue(maxsize=1)
    worker = context.Process(
        target=_crawl_worker,
        args=(sources, options.model_dump(), output),
        daemon=True,
    )
    worker.start()
    worker.join(timeout_seconds)
    if worker.is_alive():
        worker.terminate()
        worker.join()
        raise TimeoutError("Crawl exceeded its invocation time budget")
    if worker.exitcode != 0:
        raise RuntimeError(f"Scrapy worker exited with status {worker.exitcode}")
    try:
        return cast(dict[str, Any], output.get(timeout=2))
    except Exception as exc:
        raise RuntimeError("Scrapy worker completed without a crawl summary") from exc
