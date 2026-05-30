"""Chọn provider theo config; tự fallback sang stub nếu thiếu key."""
from __future__ import annotations

import structlog

from app.config import get_settings
from app.providers.base import EmbeddingProvider, LLMProvider

log = structlog.get_logger(__name__)


def build_llm() -> LLMProvider:
    s = get_settings()
    provider = s.llm_provider.lower()
    try:
        if provider == "openai" and s.openai_api_key:
            from app.providers.openai_llm import OpenAILLM

            return OpenAILLM()
        if provider == "gemini" and s.gemini_api_key:
            from app.providers.gemini_llm import GeminiLLM

            return GeminiLLM()
        if provider in {"openai", "gemini"}:
            log.warning("llm_key_missing_fallback_stub", provider=provider)
    except Exception as e:  # thiếu dependency/khởi tạo lỗi → không chặn startup
        log.error("llm_build_failed_fallback_stub", provider=provider, err=str(e))
    from app.providers.stub_llm import StubLLM

    return StubLLM()


def build_fallback_llm() -> LLMProvider | None:
    """LLM dự phòng khi provider chính lỗi (Gemini nếu có key). Best-effort."""
    s = get_settings()
    if s.llm_provider.lower() != "gemini" and s.gemini_api_key:
        try:
            from app.providers.gemini_llm import GeminiLLM

            return GeminiLLM()
        except Exception as e:
            log.warning("fallback_llm_unavailable", err=str(e))
    return None


def build_embedder() -> EmbeddingProvider:
    s = get_settings()
    if s.embedding_provider.lower() == "openai" and s.openai_api_key:
        # Optional path; mặc định là local theo quyết định của user.
        from app.providers.openai_embed import OpenAIEmbed

        return OpenAIEmbed()
    from app.providers.local_embed import LocalEmbed

    return LocalEmbed()
