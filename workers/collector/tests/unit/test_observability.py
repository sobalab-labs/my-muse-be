import json
import logging

from my_muse_collector.infrastructure.observability import JsonLogFormatter


def test_framework_logs_do_not_emit_dynamic_message_payloads() -> None:
    record = logging.LogRecord(
        name="scrapy.downloadermiddlewares.retry",
        level=logging.WARNING,
        pathname="",
        lineno=0,
        msg="retrying https://example.com/?token=private",
        args=(),
        exc_info=None,
    )

    output = JsonLogFormatter().format(record)
    parsed = json.loads(output)

    assert parsed["event"] == "framework_log"
    assert "private" not in output
