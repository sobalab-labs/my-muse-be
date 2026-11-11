from datetime import datetime
from uuid import UUID

import pytest
from pydantic import ValidationError

from my_muse_collector.domain.models import ConcertCandidate, WebsiteSource


def test_website_source_rejects_unreviewed_fields() -> None:
    with pytest.raises(ValidationError):
        WebsiteSource.model_validate(
            {
                "id": "ab3d980b-8ce1-445d-b6f1-a442ae01a9c3",
                "artist_id": "1d1e69d3-e0db-42e3-a786-680e955a8e83",
                "url": "https://example.com/events",
                "strategy_type": "json_ld",
                "config": {},
                "python_code": "arbitrary code must not be accepted",
            }
        )


def test_concert_candidate_requires_timezone_aware_start() -> None:
    with pytest.raises(ValidationError, match="timezone"):
        ConcertCandidate(
            title="Show",
            start_datetime=datetime(2027, 5, 12, 19, 30),
            announcement_url="https://example.com/event",
            source_url="https://example.com/events",
            artist_id=UUID("1d1e69d3-e0db-42e3-a786-680e955a8e83"),
        )
