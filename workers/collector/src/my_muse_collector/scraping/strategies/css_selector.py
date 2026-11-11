import logging
from collections.abc import Sequence
from datetime import date, datetime, time
from urllib.parse import urljoin
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import ValidationError
from scrapy.http import Response

from my_muse_collector.domain.models import ConcertCandidate, WebsiteSource
from my_muse_collector.schemas.source_config import CssSelectorConfig

logger = logging.getLogger(__name__)


class CssSelectorStrategy:
    def validate_config(self, source: WebsiteSource) -> None:
        config = CssSelectorConfig.model_validate(source.config)
        if config.timezone:
            ZoneInfo(config.timezone)

    def start_urls(self, source: WebsiteSource) -> Sequence[str]:
        config = CssSelectorConfig.model_validate(source.config)
        return [str(url) for url in config.start_urls] or [str(source.url)]

    def extract(self, response: Response, source: WebsiteSource) -> Sequence[ConcertCandidate]:
        config = CssSelectorConfig.model_validate(source.config)
        selectors = config.selectors
        result: list[ConcertCandidate] = []
        for container in response.css(selectors.event_container):
            title = _value(container, selectors.title)
            raw_date = _value(container, selectors.date)
            try:
                start = _parse_date(raw_date, config.date_formats, config.timezone)
                announcement = (
                    _value(container, selectors.announcement_url)
                    if selectors.announcement_url
                    else None
                )
                ticket = _value(container, selectors.ticket_url) if selectors.ticket_url else None
                result.append(
                    ConcertCandidate(
                        title=title,
                        start_datetime=start,
                        venue_name=_value(container, selectors.venue) if selectors.venue else None,
                        location_text=_value(container, selectors.location)
                        if selectors.location
                        else None,
                        ticketing_url=urljoin(response.url, ticket) if ticket else None,
                        announcement_url=urljoin(response.url, announcement)
                        if announcement
                        else response.url,
                        source_url=source.url,
                        artist_id=source.artist_id,
                        external_event_id=_value(container, selectors.external_event_id)
                        if selectors.external_event_id
                        else None,
                        extraction_metadata={"strategy": "css_selector", **config.metadata},
                    )
                )
            except (ValidationError, ValueError, TypeError, ZoneInfoNotFoundError) as exc:
                logger.info(
                    "css_event_rejected",
                    extra={"source_id": str(source.id), "error_type": type(exc).__name__},
                )
        return result


def _value(node: object, selector: str | None) -> str | None:
    if not selector:
        return None
    selection = node.css(selector)  # type: ignore[attr-defined]
    value = selection.get()
    if value is None:
        value = selection.xpath("string(.)").get()
    return value.strip() if value and value.strip() else None


def _parse_date(raw: str | None, formats: list[str], timezone: str | None) -> datetime:
    if not raw:
        raise ValueError("event date is empty")
    parsed: datetime | None = None
    try:
        parsed = datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except ValueError:
        for date_format in formats:
            try:
                parsed = datetime.strptime(raw, date_format)
                break
            except ValueError:
                continue
        if parsed is None:
            try:
                parsed = datetime.combine(date.fromisoformat(raw), time.min)
            except ValueError as exc:
                raise ValueError("date did not match ISO-8601 or a configured date format") from exc
    if parsed.tzinfo is None:
        if not timezone:
            raise ValueError("date has no timezone and source config has no timezone")
        parsed = parsed.replace(tzinfo=ZoneInfo(timezone))
    return parsed
