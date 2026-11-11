from collections.abc import Sequence
from uuid import UUID

import pytest

from my_muse_collector.application import orchestrator
from my_muse_collector.domain.models import ConcertCandidate, WebsiteSource


def _candidate(title: str) -> ConcertCandidate:
    return ConcertCandidate(
        title=title,
        start_datetime="2027-05-12T19:30:00+09:00",
        announcement_url=f"https://example.com/{title.replace(' ', '-')}",
        source_url="https://example.com/events",
        artist_id=UUID("1d1e69d3-e0db-42e3-a786-680e955a8e83"),
    )


class FakeSourceRepository:
    async def list_enabled_sources(self, limit: int) -> Sequence[WebsiteSource]:
        assert limit == 3
        return [
            WebsiteSource(
                id=UUID("ab3d980b-8ce1-445d-b6f1-a442ae01a9c3"),
                artist_id=UUID("1d1e69d3-e0db-42e3-a786-680e955a8e83"),
                url="https://example.com/events",
                strategy_type="json_ld",
            )
        ]


class FakeIngestionClient:
    def __init__(self) -> None:
        self.batches: list[list[ConcertCandidate]] = []

    async def submit(self, candidates: Sequence[ConcertCandidate]) -> dict[str, int]:
        self.batches.append(list(candidates))
        return {"received": len(candidates), "created": len(candidates)}


@pytest.mark.asyncio
async def test_collection_ingests_candidates_in_bounded_batches(monkeypatch) -> None:
    source = (await FakeSourceRepository().list_enabled_sources(3))[0]
    monkeypatch.setattr(
        orchestrator.settings,
        "ingestion_batch_size",
        2,
    )
    monkeypatch.setattr(
        orchestrator,
        "crawl",
        lambda sources, timeout: {
            "candidates": [
                _candidate(f"Show {index}").model_dump(mode="json") for index in range(5)
            ],
            "source_outcomes": {
                str(source.id): {
                    "source_id": str(source.id),
                    "status": "success",
                    "http_status": 200,
                    "candidate_count": 5,
                }
            },
        },
    )
    ingestion = FakeIngestionClient()

    result = await orchestrator.run_collection(
        FakeSourceRepository(), ingestion, source_limit=3, crawl_timeout_seconds=30
    )

    assert [len(batch) for batch in ingestion.batches] == [2, 2, 1]
    assert result["candidates"] == 5
    assert result["ingestion"] == {"received": 5, "created": 5}
