from my_muse_collector.scraping.strategies.base import ExtractionStrategy
from my_muse_collector.scraping.strategies.css_selector import CssSelectorStrategy
from my_muse_collector.scraping.strategies.json_ld import JsonLdStrategy


class UnknownStrategyError(ValueError):
    pass


class StrategyRegistry:
    def __init__(self) -> None:
        self._strategies: dict[str, ExtractionStrategy] = {
            "json_ld": JsonLdStrategy(),
            "css_selector": CssSelectorStrategy(),
        }

    def resolve(self, strategy_type: str) -> ExtractionStrategy:
        try:
            return self._strategies[strategy_type]
        except KeyError as exc:
            raise UnknownStrategyError(f"Unsupported extraction strategy: {strategy_type}") from exc
