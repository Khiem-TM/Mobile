"""Interface cho LLM generation và embedding (cho phép thay provider)."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import AsyncIterator, Protocol


@dataclass
class LLMResult:
    text: str
    tokens_in: int = 0
    tokens_out: int = 0
    model: str = ""
    cost_usd: float = 0.0


@dataclass
class Message:
    role: str  # "system" | "user" | "assistant"
    content: str


class LLMProvider(Protocol):
    name: str

    async def generate(self, messages: list[Message]) -> LLMResult: ...

    async def stream(self, messages: list[Message]) -> AsyncIterator[str]: ...


class EmbeddingProvider(Protocol):
    name: str
    dim: int

    async def embed(self, texts: list[str], *, is_query: bool = False) -> list[list[float]]: ...


# Bảng giá thô (USD / 1K tokens) để ước lượng cost. Cập nhật khi đổi model.
PRICING: dict[str, tuple[float, float]] = {
    "gpt-4o-mini": (0.00015, 0.00060),
    "gpt-4o": (0.0025, 0.010),
    "gpt-4.1": (0.0020, 0.008),
    "gemini-2.5-flash": (0.0, 0.0),  # free tier / không tính
}


def estimate_cost(model: str, tokens_in: int, tokens_out: int) -> float:
    p_in, p_out = PRICING.get(model, (0.0, 0.0))
    return round(p_in * tokens_in / 1000 + p_out * tokens_out / 1000, 6)
