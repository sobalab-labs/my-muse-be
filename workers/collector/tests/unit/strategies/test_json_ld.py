import logging
from pathlib import Path
from uuid import UUID

from scrapy.http import HtmlResponse, Request

from my_muse_collector.domain.models import WebsiteSource
from my_muse_collector.scraping.strategies.json_ld import JsonLdStrategy

FIXTURES = Path(__file__).parents[2] / "fixtures"


def _source() -> WebsiteSource:
    return WebsiteSource(
        id=UUID("ab3d980b-8ce1-445d-b6f1-a442ae01a9c3"),
        artist_id=UUID("1d1e69d3-e0db-42e3-a786-680e955a8e83"),
        url="https://example.com/events",
        strategy_type="json_ld",
    )


def test_extracts_music_event_and_normalizes_links() -> None:
    body = (FIXTURES / "json_ld" / "music_event.html").read_bytes()
    response = HtmlResponse(
        url="https://example.com/events",
        request=Request("https://example.com/events"),
        body=body,
        encoding="utf-8",
    )

    candidates = JsonLdStrategy().extract(response, _source())

    assert len(candidates) == 1
    candidate = candidates[0]
    assert candidate.title == "Example Live"
    assert candidate.venue_name == "Example Hall"
    assert candidate.location_text == "Seoul, KR"
    assert str(candidate.ticketing_url) == "https://example.com/tickets/example-live"
    assert str(candidate.announcement_url) == "https://example.com/events/example-live"
    assert candidate.external_event_id == "concert-123"
    assert candidate.start_datetime.utcoffset().total_seconds() == 9 * 60 * 60


def test_ignores_malformed_json_and_events_without_timezone(caplog) -> None:
    caplog.set_level(logging.INFO)
    body = b"""<script type="application/ld+json">{broken</script>
    <script type="application/ld+json">
    {"@type":"Event","name":"No timezone","startDate":"2027-05-12T19:30:00"}
    </script>"""
    response = HtmlResponse(
        url="https://example.com/events",
        request=Request("https://example.com/events"),
        body=body,
        encoding="utf-8",
    )

    assert JsonLdStrategy().extract(response, _source()) == []
    assert "json_ld_invalid_document" in caplog.text
    assert "json_ld_event_rejected" in caplog.text
