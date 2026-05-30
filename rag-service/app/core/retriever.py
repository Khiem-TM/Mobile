"""Retrieval pipeline (plan mục 9): intent → embed query → vector search → filter."""
from __future__ import annotations

import time

import structlog

from app.core.embedding_service import EmbeddingService
from app.core.intent import SOURCE_BY_INTENT, TOP_K_BY_INTENT
from app.db.rag_repo import RagRepo, RetrievedChunk

log = structlog.get_logger(__name__)


class Retriever:
    def __init__(self, repo: RagRepo, embedder: EmbeddingService) -> None:
        self.repo = repo
        self.embedder = embedder

    async def retrieve(self, intent: str, query: str) -> tuple[list[RetrievedChunk], int]:
        top_k = TOP_K_BY_INTENT.get(intent, 5)
        if top_k == 0:
            return [], 0
        started = time.perf_counter()
        query_vec = await self.embedder.embed_query(query)
        source = SOURCE_BY_INTENT.get(intent)
        chunks = await self.repo.search(query_vec, top_k=top_k, source=source)
        latency_ms = int((time.perf_counter() - started) * 1000)
        log.info("retrieval", intent=intent, top_k=top_k, hits=len(chunks), latency_ms=latency_ms)
        return chunks, latency_ms
