"""Embedding qua OpenAI — tuỳ chọn (mặc định dùng local). Lưu ý: dim=1536
với text-embedding-3-small ⇒ phải đổi EMBED_DIM và reindex pgvector."""
from __future__ import annotations

from openai import AsyncOpenAI

from app.config import get_settings
from app.providers.base import EmbeddingProvider


class OpenAIEmbed(EmbeddingProvider):
    name = "openai"

    def __init__(self) -> None:
        s = get_settings()
        self.dim = s.embed_dim
        self.model = "text-embedding-3-small"
        self._client = AsyncOpenAI(api_key=s.openai_api_key)

    async def embed(self, texts: list[str], *, is_query: bool = False) -> list[list[float]]:
        resp = await self._client.embeddings.create(model=self.model, input=texts)
        return [d.embedding for d in resp.data]
