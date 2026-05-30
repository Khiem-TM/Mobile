"""Bọc EmbeddingProvider + cache query embedding trong Redis."""
from __future__ import annotations

import hashlib
import json

import structlog

from app.providers.base import EmbeddingProvider

log = structlog.get_logger(__name__)


class EmbeddingService:
    def __init__(self, provider: EmbeddingProvider, redis=None) -> None:
        self.provider = provider
        self.redis = redis

    @property
    def dim(self) -> int:
        return self.provider.dim

    async def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return await self.provider.embed(texts, is_query=False)

    async def embed_query(self, text: str) -> list[float]:
        key = "emb:q:" + hashlib.sha256(text.strip().lower().encode()).hexdigest()
        if self.redis is not None:
            try:
                cached = await self.redis.get(key)
                if cached:
                    return json.loads(cached)
            except Exception as e:  # cache không bắt buộc
                log.warning("embed_cache_get_failed", err=str(e))
        vec = (await self.provider.embed([text], is_query=True))[0]
        if self.redis is not None:
            try:
                await self.redis.set(key, json.dumps(vec), ex=7 * 24 * 3600)
            except Exception as e:
                log.warning("embed_cache_set_failed", err=str(e))
        return vec
