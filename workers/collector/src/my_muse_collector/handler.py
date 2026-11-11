"""AWS Lambda adapter. Runtime repository/client wiring is deployment-specific."""

from collections.abc import Mapping
from typing import Any


def handler(event: Mapping[str, Any], context: Any) -> dict[str, Any]:
    """Validate a scheduled invocation and delegate to configured application services."""
    if not isinstance(event, Mapping):
        raise ValueError("Lambda event must be an object")
    raise RuntimeError(
        "Collector source repository and ingestion client are not configured; "
        "implement deployment adapters for the approved backend contract."
    )
