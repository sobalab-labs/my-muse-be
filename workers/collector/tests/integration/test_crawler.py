from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Thread
from uuid import UUID

from my_muse_collector.domain.models import WebsiteSource
from my_muse_collector.scraping.crawler import crawl
from my_muse_collector.settings import Settings


class FixtureHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        if self.path == "/events":
            body = b"""<script type="application/ld+json">
            {"@type":"Event","name":"Local Show",
             "startDate":"2027-05-12T19:30:00+09:00"}
            </script>"""
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        self.send_error(404)

    def log_message(self, format: str, *args: object) -> None:
        return


def test_crawler_fetches_source_in_fresh_worker_process() -> None:
    server = ThreadingHTTPServer(("127.0.0.1", 0), FixtureHandler)
    thread = Thread(target=server.serve_forever, daemon=True)
    thread.start()
    source = WebsiteSource(
        id=UUID("ab3d980b-8ce1-445d-b6f1-a442ae01a9c3"),
        artist_id=UUID("1d1e69d3-e0db-42e3-a786-680e955a8e83"),
        url=f"http://127.0.0.1:{server.server_port}/events",
        strategy_type="json_ld",
    )

    try:
        result = crawl(
            [source.model_dump(mode="json")],
            options=Settings(
                _env_file=None,
                crawl_timeout_seconds=30,
                download_timeout_seconds=5,
                concurrent_requests=2,
                per_domain_concurrency=1,
            ),
        )
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=2)

    assert result["item_count"] == 1, result
    assert result["candidates"][0]["title"] == "Local Show"
    assert result["source_outcomes"][str(source.id)]["status"] == "success"
