"""Gemini generation provider — dùng làm fallback (key đã có trong repo)."""
from __future__ import annotations

from typing import AsyncIterator

from app.config import get_settings
from app.providers.base import LLMProvider, LLMResult, Message, estimate_cost


class GeminiLLM(LLMProvider):
    name = "gemini"

    def __init__(self) -> None:
        import google.generativeai as genai  # import trễ để không bắt buộc cài

        s = get_settings()
        genai.configure(api_key=s.gemini_api_key)
        self.model_name = s.gemini_model
        self._genai = genai
        self._model = genai.GenerativeModel(s.gemini_model)

    @staticmethod
    def _to_prompt(messages: list[Message]) -> str:
        # Gemini không có "system role" tách bạch như OpenAI -> gộp thành 1 prompt.
        parts = []
        for m in messages:
            prefix = {"system": "[HỆ THỐNG]", "user": "[NGƯỜI DÙNG]", "assistant": "[TRỢ LÝ]"}.get(m.role, "")
            parts.append(f"{prefix}\n{m.content}")
        return "\n\n".join(parts)

    async def generate(self, messages: list[Message]) -> LLMResult:
        resp = await self._model.generate_content_async(self._to_prompt(messages))
        text = getattr(resp, "text", "") or ""
        return LLMResult(text=text, model=self.model_name, cost_usd=estimate_cost(self.model_name, 0, 0))

    async def stream(self, messages: list[Message]) -> AsyncIterator[str]:
        resp = await self._model.generate_content_async(self._to_prompt(messages), stream=True)
        async for chunk in resp:
            if getattr(chunk, "text", None):
                yield chunk.text
