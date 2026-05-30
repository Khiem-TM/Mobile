"""OpenAI generation provider (default cho LLM)."""
from __future__ import annotations

from typing import AsyncIterator

from openai import AsyncOpenAI

from app.config import get_settings
from app.providers.base import LLMProvider, LLMResult, Message, estimate_cost


class OpenAILLM(LLMProvider):
    name = "openai"

    def __init__(self) -> None:
        s = get_settings()
        self.model = s.openai_llm_model
        self.temperature = s.llm_temperature
        self.max_tokens = s.llm_max_tokens
        self._client = AsyncOpenAI(api_key=s.openai_api_key, timeout=s.llm_timeout_seconds)

    def _payload(self, messages: list[Message]) -> list[dict]:
        return [{"role": m.role, "content": m.content} for m in messages]

    async def generate(self, messages: list[Message]) -> LLMResult:
        resp = await self._client.chat.completions.create(
            model=self.model,
            messages=self._payload(messages),
            temperature=self.temperature,
            max_tokens=self.max_tokens,
        )
        usage = resp.usage
        tin = usage.prompt_tokens if usage else 0
        tout = usage.completion_tokens if usage else 0
        return LLMResult(
            text=resp.choices[0].message.content or "",
            tokens_in=tin,
            tokens_out=tout,
            model=self.model,
            cost_usd=estimate_cost(self.model, tin, tout),
        )

    async def stream(self, messages: list[Message]) -> AsyncIterator[str]:
        stream = await self._client.chat.completions.create(
            model=self.model,
            messages=self._payload(messages),
            temperature=self.temperature,
            max_tokens=self.max_tokens,
            stream=True,
        )
        async for chunk in stream:
            delta = chunk.choices[0].delta.content if chunk.choices else None
            if delta:
                yield delta
