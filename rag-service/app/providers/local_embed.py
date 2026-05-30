"""Embedding chạy local bằng sentence-transformers (mặc định).

Model `intfloat/multilingual-e5-base` (768-dim, tốt tiếng Việt). e5 yêu cầu
prefix "query: " cho truy vấn và "passage: " cho đoạn văn được index.

Nếu không tải được model (môi trường dev/CI nhẹ), tự rơi về `HashEmbed`
(băm xác định) để dịch vụ vẫn chạy được — KHÔNG dùng cho production.
"""
from __future__ import annotations

import asyncio
import hashlib
import math

from app.config import get_settings
from app.providers.base import EmbeddingProvider


class LocalEmbed(EmbeddingProvider):
    name = "local"

    def __init__(self) -> None:
        s = get_settings()
        self.dim = s.embed_dim
        self.batch_size = s.embed_batch_size
        self._model = None
        try:
            from sentence_transformers import SentenceTransformer

            self._model = SentenceTransformer(s.embed_model)
            self.dim = self._model.get_sentence_embedding_dimension()
        except Exception:  # pragma: no cover - fallback dev
            self._model = None

    async def embed(self, texts: list[str], *, is_query: bool = False) -> list[list[float]]:
        prefix = "query: " if is_query else "passage: "
        prepared = [prefix + t for t in texts]
        if self._model is None:
            return [_hash_embed(t, self.dim) for t in texts]
        # SentenceTransformer.encode là sync/CPU-bound -> đẩy sang thread.
        return await asyncio.to_thread(self._encode, prepared)

    def _encode(self, texts: list[str]) -> list[list[float]]:
        vecs = self._model.encode(
            texts, batch_size=self.batch_size, normalize_embeddings=True, convert_to_numpy=True
        )
        return [v.tolist() for v in vecs]


def _hash_embed(text: str, dim: int) -> list[float]:
    """Embedding xác định dựa trên hash — chỉ để dev chạy được, không có ý nghĩa ngữ nghĩa."""
    vec = [0.0] * dim
    for token in text.lower().split():
        h = int(hashlib.md5(token.encode()).hexdigest(), 16)
        vec[h % dim] += 1.0
    norm = math.sqrt(sum(v * v for v in vec)) or 1.0
    return [v / norm for v in vec]
