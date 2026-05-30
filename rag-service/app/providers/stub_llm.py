"""LLM giả lập để chạy/dev khi chưa cấu hình API key (không gọi mạng)."""
from __future__ import annotations

from typing import AsyncIterator

from app.providers.base import LLMProvider, LLMResult, Message


class StubLLM(LLMProvider):
    name = "stub"

    async def generate(self, messages: list[Message]) -> LLMResult:
        user_msg = next((m.content for m in reversed(messages) if m.role == "user"), "")
        text = (
            "**(Chế độ DEV — chưa cấu hình OPENAI_API_KEY)**\n\n"
            f"Bạn vừa hỏi: “{user_msg[:200]}”. Đây là phản hồi mẫu của Vital Coach. "
            "Hãy đặt `OPENAI_API_KEY` để nhận tư vấn thật."
        )
        return LLMResult(text=text, model="stub", tokens_in=0, tokens_out=0)

    async def stream(self, messages: list[Message]) -> AsyncIterator[str]:
        result = await self.generate(messages)
        for word in result.text.split(" "):
            yield word + " "
