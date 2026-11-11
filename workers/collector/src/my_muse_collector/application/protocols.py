from collections.abc import Sequence
from typing import Protocol

from my_muse_collector.domain.models import ConcertCandidate, WebsiteSource


class SourceRepository(Protocol):
    async def list_enabled_sources(self, limit: int) -> Sequence[WebsiteSource]: ...


class ConcertIngestionClient(Protocol):
    async def submit(self, candidates: Sequence[ConcertCandidate]) -> dict[str, int]: ...
