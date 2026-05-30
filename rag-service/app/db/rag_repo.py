"""CRUD + vector search trên schema `rag`."""
from __future__ import annotations

import uuid
from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import (
    ConversationMemory,
    PromptLog,
    ProcessedEvent,
    RagChunk,
    RagDocument,
    RetrievalLog,
    UserAiDailySummary,
    UserAiWeeklyReport,
)


@dataclass
class RetrievedChunk:
    id: uuid.UUID
    document_id: uuid.UUID
    title: str
    content: str
    chunk_index: int
    score: float
    metadata: dict


class RagRepo:
    def __init__(self, session: AsyncSession) -> None:
        self.s = session

    # ── Ingestion ───────────────────────────────────────────────────────────
    async def create_document(
        self, *, source: str, title: str, lang: str = "vi", quality: str = "curated",
        uri: str | None = None, checksum: str | None = None,
    ) -> RagDocument:
        doc = RagDocument(source=source, title=title, lang=lang, quality=quality, uri=uri, checksum=checksum)
        self.s.add(doc)
        await self.s.flush()
        return doc

    async def add_chunks(
        self, document_id: uuid.UUID, chunks: list[tuple[str, list[float], dict]]
    ) -> int:
        for idx, (content, embedding, meta) in enumerate(chunks):
            self.s.add(
                RagChunk(
                    document_id=document_id,
                    chunk_index=idx,
                    content=content,
                    token_count=len(content.split()),
                    embedding=embedding,
                    meta=meta,
                )
            )
        await self.s.flush()
        return len(chunks)

    # ── Retrieval (cosine distance qua pgvector) ────────────────────────────
    async def search(
        self, query_embedding: list[float], *, top_k: int = 5, source: str | None = None
    ) -> list[RetrievedChunk]:
        distance = RagChunk.embedding.cosine_distance(query_embedding).label("distance")
        stmt = (
            select(RagChunk, RagDocument.title, distance)
            .join(RagDocument, RagDocument.id == RagChunk.document_id)
            .order_by(distance)
            .limit(top_k)
        )
        if source:
            stmt = stmt.where(RagChunk.meta["source"].astext == source)
        rows = (await self.s.execute(stmt)).all()
        return [
            RetrievedChunk(
                id=chunk.id,
                document_id=chunk.document_id,
                title=title,
                content=chunk.content,
                chunk_index=chunk.chunk_index,
                score=1.0 - float(dist),  # cosine similarity
                metadata=chunk.meta or {},
            )
            for chunk, title, dist in rows
        ]

    # ── Memory ──────────────────────────────────────────────────────────────
    async def get_memory(self, user_ref: str) -> ConversationMemory | None:
        return await self.s.get(ConversationMemory, user_ref)

    async def upsert_memory(self, user_ref: str, summary: str, facts: list | None = None) -> None:
        stmt = pg_insert(ConversationMemory).values(
            user_ref=user_ref, summary=summary, salient_facts=facts or []
        )
        stmt = stmt.on_conflict_do_update(
            index_elements=[ConversationMemory.user_ref],
            set_={"summary": summary, "salient_facts": facts or []},
        )
        await self.s.execute(stmt)

    # ── Daily summary / Weekly report (P2) ──────────────────────────────────
    async def upsert_daily_summary(self, user_ref: str, date, summary: str, metrics: dict) -> None:
        stmt = pg_insert(UserAiDailySummary).values(
            user_ref=user_ref, date=date, summary=summary, metrics=metrics
        )
        stmt = stmt.on_conflict_do_update(
            index_elements=[UserAiDailySummary.user_ref, UserAiDailySummary.date],
            set_={"summary": summary, "metrics": metrics},
        )
        await self.s.execute(stmt)

    async def get_daily_summary(self, user_ref: str, date) -> UserAiDailySummary | None:
        return await self.s.get(UserAiDailySummary, (user_ref, date))

    async def upsert_weekly_report(self, user_ref: str, week_start, report: dict) -> None:
        stmt = pg_insert(UserAiWeeklyReport).values(
            user_ref=user_ref, week_start=week_start, report=report
        )
        stmt = stmt.on_conflict_do_update(
            index_elements=[UserAiWeeklyReport.user_ref, UserAiWeeklyReport.week_start],
            set_={"report": report},
        )
        await self.s.execute(stmt)

    async def get_weekly_report(self, user_ref: str, week_start) -> UserAiWeeklyReport | None:
        return await self.s.get(UserAiWeeklyReport, (user_ref, week_start))

    # ── Logs ────────────────────────────────────────────────────────────────
    async def log_retrieval(self, **kw) -> None:
        self.s.add(RetrievalLog(**kw))

    async def log_prompt(self, **kw) -> None:
        self.s.add(PromptLog(**kw))

    # ── Idempotency (Kafka, Phase 3) ────────────────────────────────────────
    async def mark_event(self, event_id: uuid.UUID) -> bool:
        """True nếu lần đầu xử lý; False nếu đã xử lý (bỏ qua)."""
        stmt = pg_insert(ProcessedEvent).values(event_id=event_id).on_conflict_do_nothing()
        res = await self.s.execute(stmt)
        return res.rowcount > 0
