"""Bọc LLMProvider chính + fallback khi lỗi/timeout (plan mục 11)."""
from __future__ import annotations

from typing import AsyncIterator

import structlog

from app.providers.base import LLMProvider, LLMResult, Message

log = structlog.get_logger(__name__)


class LLMService:
    def __init__(self, primary: LLMProvider, fallback: LLMProvider | None = None) -> None:
        self.primary = primary
        self.fallback = fallback

    async def generate(self, messages: list[Message]) -> LLMResult:
        try:
            return await self.primary.generate(messages)
        except Exception as e:
            log.error("llm_primary_failed", provider=self.primary.name, err=str(e))
            if self.fallback is not None:
                log.info("llm_fallback", provider=self.fallback.name)
                return await self.fallback.generate(messages)
            raise

    async def stream(self, messages: list[Message]) -> AsyncIterator[str]:
        try:
            async for delta in self.primary.stream(messages):
                yield delta
        except Exception as e:  # pragma: no cover
            log.error("llm_stream_failed", provider=self.primary.name, err=str(e))
            if self.fallback is not None:
                async for delta in self.fallback.stream(messages):
                    yield delta
            else:
                yield "\n\n(Xin lỗi, trợ lý đang bận. Vui lòng thử lại sau.)"
