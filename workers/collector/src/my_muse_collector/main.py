import asyncio


async def run() -> None:
    raise RuntimeError(
        "Collector source repository and ingestion client are not configured; "
        "implement deployment adapters for the approved backend contract."
    )


def main() -> None:
    asyncio.run(run())


if __name__ == "__main__":
    main()
