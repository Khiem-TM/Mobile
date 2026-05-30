"""Pydantic schema cho request/response — khớp contract NestJS hiện tại.

NestJS `ChatbotService.sendMessage` gọi:
    POST /chat { user_id, session_id, message, conversation_history }
và đọc `response.data.reply`. Ta giữ nguyên `reply` để không phá vỡ tích hợp,
đồng thời bổ sung các trường structured (intent/sources/safety/suggestions)
để mobile có thể render phong phú hơn khi NestJS chuẩn hoá lại response.
"""
from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


class Turn(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    user_id: str
    session_id: str | None = None
    message: str
    conversation_history: list[Turn] = Field(default_factory=list)
    # NestJS sẽ gửi UserContext đã mask PII (xem plan mục 3). Optional ở P1.
    user_context: dict[str, Any] | None = None
    request_id: str | None = None


class Source(BaseModel):
    title: str
    document_id: str
    chunk_index: int | None = None


class Safety(BaseModel):
    flagged: bool = False
    category: str | None = None
    disclaimer: str | None = None


class TokenUsage(BaseModel):
    tokens_in: int = 0
    tokens_out: int = 0
    cost_usd: float = 0.0
    model: str = ""


class ChatResponse(BaseModel):
    reply: str  # ← NestJS đọc trường này
    intent: str = "smalltalk"
    answer_markdown: str = ""
    suggestions: list[str] = Field(default_factory=list)
    sources: list[Source] = Field(default_factory=list)
    safety: Safety = Field(default_factory=Safety)
    usage: TokenUsage = Field(default_factory=TokenUsage)
    request_id: str | None = None


class EmbedDocumentRequest(BaseModel):
    source: Literal["nutrition", "workout", "mental", "faq"]
    title: str
    content: str
    lang: str = "vi"
    quality: str = "curated"
    metadata: dict[str, Any] = Field(default_factory=dict)


class EmbedDocumentResponse(BaseModel):
    document_id: str
    chunks: int
