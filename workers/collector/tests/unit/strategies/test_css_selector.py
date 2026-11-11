import logging
from pathlib import Path
from uuid import UUID

from scrapy.http import HtmlResponse, Request

from my_muse_collector.domain.models import WebsiteSource
from my_muse_collector.scraping.strategies.css_selector import CssSelectorStrategy

FIXTURE = Path(__file__).parents[2] / "fixtures" / "websites" / "css_events.html"


def _source(timezone: str | None = "Asia/Seoul") -> WebsiteSource:
    config = {
        "selectors": {
            "event_container": "article.event",
            "title": ".title::text",
            "date": ".date::attr(datetime)",
            "venue": ".venue::text",
            "ticket_url": "a.ticket::attr(href)",
            "announcement_url": "a.announcement::attr(href)",
        },
        "timezone": timezone,
    }
    return WebsiteSource(
        id=UUID("ab3d980b-8ce1-445d-b6f1-a442ae01a9c3"),
        artist_id=UUID("1d1e69d3-e0db-42e3-a786-680e955a8e83"),
        url="https://example.com/events",
        strategy_type="css_selector",
        config=config,
    )


def _response() -> HtmlResponse:
    return HtmlResponse(
        url="https://example.com/events",
        request=Request("https://example.com/events"),
        body=FIXTURE.read_bytes(),
        encoding="utf-8",
    )


def test_extracts_configured_events_with_timezone_and_relative_urls() -> None:
    candidates = CssSelectorStrategy().extract(_response(), _source())

    assert len(candidates) == 2
    first = candidates[0]
    second = candidates[1]
    assert first.title == "First show"
    assert first.venue_name == "Example Hall"
    assert first.start_datetime.utcoffset().total_seconds() == 9 * 60 * 60
    assert str(first.announcement_url) == "https://example.com/events/first"
    assert str(first.ticketing_url) == "https://example.com/tickets/first"
    assert second.ticketing_url is None
    assert second.start_datetime.utcoffset().total_seconds() == 9 * 60 * 60


def test_rejects_naive_dates_without_configured_timezone(caplog) -> None:
    caplog.set_level(logging.INFO)
    candidates = CssSelectorStrategy().extract(_response(), _source(timezone=None))

    assert len(candidates) == 1
    assert candidates[0].title == "Second show"
    assert "css_event_rejected" in caplog.text
