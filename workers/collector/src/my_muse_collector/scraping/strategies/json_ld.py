import json
import logging
from collections.abc import Iterator, Mapping, Sequence
from typing import Any
from urllib.parse import urljoin

from pydantic import ValidationError
from scrapy.http import Response

from my_muse_collector.domain.models import ConcertCandidate, WebsiteSource

logger = logging.getLogger(__name__)


def _events(value: Any) -> Iterator[Mapping[str, Any]]:
    if isinstance(value, list):
        for item in value:
            yield from _events(item)
    elif isinstance(value, dict):
        graph = value.get("@graph")
        if graph is not None:
            yield from _events(graph)
        kind = value.get("@type", [])
        kinds = {kind} if isinstance(kind, str) else set(kind)
        if any(str(item).endswith("Event") for item in kinds):
            yield value
        for key in ("itemListElement", "mainEntity", "events"):
            if key in value:
                yield from _events(value[key])


class JsonLdStrategy:
    def validate_config(self, source: WebsiteSource) -> None:
        if source.config:
            raise ValueError("json_ld strategy does not accept configuration fields")

    def start_urls(self, source: WebsiteSource) -> Sequence[str]:
        return [str(source.url)]

    def extract(self, response: Response, source: WebsiteSource) -> Sequence[ConcertCandidate]:
        candidates: list[ConcertCandidate] = []
        for script in response.css('script[type="application/ld+json"]::text').getall():
            try:
                data = json.loads(script)
            except json.JSONDecodeError:
                logger.info("json_ld_invalid_document", extra={"source_id": str(source.id)})
                continue
            for event in _events(data):
                try:
                    candidates.append(
                        ConcertCandidate(
                            title=event.get("name"),
                            start_datetime=event.get("startDate"),
                            venue_name=_venue_name(event.get("location")),
                            location_text=_location_text(event.get("location")),
                            ticketing_url=_absolute(response.url, _url(event.get("offers"))),
                            announcement_url=_absolute(
                                response.url, _url(event.get("url")) or response.url
                            ),
                            source_url=source.url,
                            artist_id=source.artist_id,
                            external_event_id=_identifier(
                                event.get("identifier") or event.get("@id")
                            ),
                            extraction_metadata={"strategy": "json_ld"},
                        )
                    )
                except (ValidationError, TypeError, ValueError) as exc:
                    logger.info(
                        "json_ld_event_rejected",
                        extra={"source_id": str(source.id), "error_type": type(exc).__name__},
                    )
        return candidates


def _venue_name(location: Any) -> str | None:
    if isinstance(location, dict):
        return location.get("name")
    return None


def _location_text(location: Any) -> str | None:
    if not isinstance(location, dict):
        return None
    address = location.get("address")
    if isinstance(address, str):
        return address
    if isinstance(address, dict):
        return (
            ", ".join(
                str(address[key])
                for key in ("addressLocality", "addressRegion", "addressCountry")
                if address.get(key)
            )
            or None
        )
    return None


def _url(value: Any) -> str | None:
    if isinstance(value, list) and value:
        return _url(value[0])
    if isinstance(value, dict):
        return value.get("url")
    return value if isinstance(value, str) else None


def _identifier(value: Any) -> str | None:
    if isinstance(value, dict):
        value = value.get("value")
    return str(value) if value is not None else None


def _absolute(base: str, value: str | None) -> str | None:
    return urljoin(base, value) if value else None
