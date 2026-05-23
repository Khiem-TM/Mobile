from typing import Literal
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Provider selection — default to Gemini
    LLM_PROVIDER: Literal["ollama", "gemini"] = "gemini"
    EMBEDDING_PROVIDER: Literal["ollama", "gemini"] = "gemini"

    # Gemini
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-2.5-flash"
    GEMINI_EMBED_MODEL: str = "gemini-embedding-001"

    # Ollama (fallback / local dev)
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_LLM_MODEL: str = "qwen2.5:7b"
    OLLAMA_EMBED_MODEL: str = "nomic-embed-text"

    # PostgreSQL (read-only)
    POSTGRES_URL: str = "postgresql+asyncpg://rag_reader:ragpass@localhost:5433/calories_tracker"

    # ChromaDB
    CHROMA_PERSIST_DIR: str = "./chroma_db"

    # Security
    NESTJS_INTERNAL_SECRET: str = "dev-secret"

    # Service
    RAG_SERVICE_PORT: int = 8001
    LOG_LEVEL: str = "INFO"


_settings: Settings | None = None


def get_settings() -> Settings:
    global _settings
    if _settings is None:
        _settings = Settings()
    return _settings
