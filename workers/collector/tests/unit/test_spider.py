from uuid import UUID

from my_muse_collector.domain.models import WebsiteSource
from my_muse_collector.scraping.spider import ConcertSpider


def test_invalid_source_strategy_isolated_before_download() -> None:
    source = WebsiteSource(
        id=UUID("ab3d980b-8ce1-445d-b6f1-a442ae01a9c3"),
        artist_id=UUID("1d1e69d3-e0db-42e3-a786-680e955a8e83"),
        url="https://example.com/events",
        strategy_type="unreviewed_dynamic_import",
    )
    spider = ConcertSpider(sources=[source.model_dump(mode="json")])

    requests = list(spider.start_requests())

    assert requests == []
    assert spider.source_outcomes[str(source.id)]["status"] == "configuration_failed"


def test_css_source_can_configure_multiple_start_urls() -> None:
    source = WebsiteSource(
        id=UUID("ab3d980b-8ce1-445d-b6f1-a442ae01a9c3"),
        artist_id=UUID("1d1e69d3-e0db-42e3-a786-680e955a8e83"),
        url="https://example.com/events",
        strategy_type="css_selector",
        config={
            "start_urls": [
                "https://example.com/events?page=1",
                "https://example.com/events?page=2",
            ],
            "selectors": {
                "event_container": ".event",
                "title": ".title::text",
                "date": ".date::text",
            },
        },
    )
    spider = ConcertSpider(sources=[source.model_dump(mode="json")])

    requests = list(spider.start_requests())

    assert [request.url for request in requests] == [
        "https://example.com/events?page=1",
        "https://example.com/events?page=2",
    ]
