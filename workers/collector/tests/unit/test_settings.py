import pytest
from pydantic import ValidationError

from my_muse_collector.settings import Settings


def test_runtime_settings_apply_bounded_defaults() -> None:
    configured = Settings(_env_file=None)

    assert configured.source_limit == 100
    assert configured.crawl_timeout_seconds == 840
    assert configured.retry_times == 2
    assert configured.max_candidates == 5_000


def test_runtime_settings_reject_inconsistent_concurrency() -> None:
    with pytest.raises(ValidationError, match="per_domain_concurrency"):
        Settings(_env_file=None, concurrent_requests=2, per_domain_concurrency=3)
