import json
import logging
from datetime import UTC, datetime
from typing import Any


class JsonLogFormatter(logging.Formatter):
    """Emit stable JSON records while excluding message arguments and payloads."""

    _fields = (
        "request_id",
        "job_id",
        "source_id",
        "correlation_id",
        "source_count",
        "candidate_count",
        "failed_source_count",
        "error_type",
        "status",
    )

    def format(self, record: logging.LogRecord) -> str:
        entry: dict[str, Any] = {
            "timestamp": datetime.fromtimestamp(record.created, UTC).isoformat(),
            "level": record.levelname.lower(),
            "logger": record.name,
            "event": (
                record.msg
                if record.name.startswith("my_muse_collector") and isinstance(record.msg, str)
                else "framework_log"
            ),
        }
        for field in self._fields:
            value = getattr(record, field, None)
            if value is not None:
                entry[field] = value
        if (
            record.exc_info
            and record.exc_info[0] is not None
            and record.name.startswith("my_muse_collector")
        ):
            entry["error_type"] = record.exc_info[0].__name__
        return json.dumps(entry, ensure_ascii=False, default=str)


def configure_logging() -> None:
    handler = logging.StreamHandler()
    handler.setFormatter(JsonLogFormatter())
    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(logging.INFO)
