from typing import Any

from pydantic import AnyHttpUrl, BaseModel, ConfigDict, Field


class CssSelectors(BaseModel):
    model_config = ConfigDict(extra="forbid")

    event_container: str = Field(min_length=1)
    title: str = Field(min_length=1)
    date: str = Field(min_length=1)
    venue: str | None = None
    location: str | None = None
    ticket_url: str | None = None
    announcement_url: str | None = None
    external_event_id: str | None = None


class CssSelectorConfig(BaseModel):
    model_config = ConfigDict(extra="forbid")

    start_urls: list[AnyHttpUrl] = Field(default_factory=list)
    selectors: CssSelectors
    date_formats: list[str] = Field(default_factory=list)
    timezone: str | None = None
    metadata: dict[str, Any] = Field(default_factory=dict)
