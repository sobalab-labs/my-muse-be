from __future__ import annotations

from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import AnyHttpUrl, BaseModel, ConfigDict, Field, field_validator


class WebsiteSource(BaseModel):
    model_config = ConfigDict(extra="forbid")

    id: UUID
    artist_id: UUID
    url: AnyHttpUrl
    strategy_type: str = Field(min_length=1)
    config: dict[str, Any] = Field(default_factory=dict)
    enabled: bool = True


class ConcertCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid")

    title: str = Field(min_length=1)
    start_datetime: datetime
    venue_name: str | None = None
    location_text: str | None = None
    ticketing_url: AnyHttpUrl | None = None
    announcement_url: AnyHttpUrl
    source_url: AnyHttpUrl
    artist_id: UUID
    external_event_id: str | None = None
    extraction_metadata: dict[str, Any] = Field(default_factory=dict)

    @field_validator("start_datetime")
    @classmethod
    def require_timezone(cls, value: datetime) -> datetime:
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("start_datetime must include a timezone or UTC offset")
        return value

    @field_validator("title", "venue_name", "location_text")
    @classmethod
    def strip_text(cls, value: str | None) -> str | None:
        if value is None:
            return None
        value = value.strip()
        return value or None
