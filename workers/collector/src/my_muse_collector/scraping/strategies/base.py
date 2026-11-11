from collections.abc import Sequence
from typing import Protocol

from scrapy.http import Response

from my_muse_collector.domain.models import ConcertCandidate, WebsiteSource


class ExtractionStrategy(Protocol):
    def validate_config(self, source: WebsiteSource) -> None: ...

    def start_urls(self, source: WebsiteSource) -> Sequence[str]: ...

    def extract(self, response: Response, source: WebsiteSource) -> Sequence[ConcertCandidate]: ...
