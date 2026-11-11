from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="COLLECTOR_", env_file=".env", extra="ignore")

    source_limit: int = Field(default=100, ge=1, le=10_000)
    crawl_timeout_seconds: int = Field(default=840, ge=1, le=840)
    download_timeout_seconds: int = Field(default=20, ge=1, le=120)
    retry_times: int = Field(default=2, ge=0, le=5)
    concurrent_requests: int = Field(default=32, ge=1, le=128)
    per_domain_concurrency: int = Field(default=4, ge=1, le=32)
    max_candidates: int = Field(default=5_000, ge=1, le=20_000)
    ingestion_batch_size: int = Field(default=100, ge=1, le=1_000)

    @model_validator(mode="after")
    def validate_concurrency(self) -> "Settings":
        if self.per_domain_concurrency > self.concurrent_requests:
            raise ValueError("per_domain_concurrency cannot exceed concurrent_requests")
        return self


settings = Settings()
