"""Cấu hình tập trung, đọc từ biến môi trường (.env)."""
from __future__ import annotations

from functools import lru_cache

from pydantic import AliasChoices, Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # Service
    app_name: str = "vitalai-rag"
    port: int = 8001
    log_level: str = "INFO"
    env: str = "development"
    nestjs_internal_secret: str = "dev-secret-change-in-prod"

    # Database
    postgres_url: str = "postgresql+asyncpg://rag_reader:ragpass@localhost:5433/calories_tracker"
    rag_schema: str = "rag"

    # Redis
    redis_url: str = "redis://localhost:6379/1"

    # LLM generation
    llm_provider: str = "openai"  # openai | gemini | stub
    openai_api_key: str = ""
    # Chấp nhận cả OPENAI_MODEL (tên trong .env của user) lẫn OPENAI_LLM_MODEL.
    openai_llm_model: str = Field(
        default="gpt-4o-mini",
        validation_alias=AliasChoices("OPENAI_LLM_MODEL", "OPENAI_MODEL", "openai_llm_model"),
    )
    llm_temperature: float = 0.3
    llm_max_tokens: int = 700
    llm_timeout_seconds: int = 30
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"

    # Embedding
    embedding_provider: str = "local"  # local | openai
    embed_model: str = "intfloat/multilingual-e5-base"
    embed_dim: int = 768
    embed_batch_size: int = 32

    # Retrieval
    retrieval_default_top_k: int = 5

    # Một số .env có comment inline sau giá trị (vd "openai   # ghi chú").
    # python-dotenv thường strip, nhưng phòng hờ: cắt phần sau '#' và khoảng trắng.
    @field_validator("llm_provider", "embedding_provider", mode="before")
    @classmethod
    def _strip_inline_comment(cls, v):
        if isinstance(v, str):
            return v.split("#", 1)[0].strip().lower()
        return v

    @property
    def is_prod(self) -> bool:
        return self.env.lower() in {"production", "prod"}


@lru_cache
def get_settings() -> Settings:
    return Settings()
