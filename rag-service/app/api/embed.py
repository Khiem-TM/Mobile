"""Ingestion endpoints: nạp tài liệu + nhận trigger re-embed user từ NestJS."""
from __future__ import annotations

import structlog
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.document_ingestion import ingest_document
from app.core.embedding_service import EmbeddingService
from app.db.rag_repo import RagRepo
from app.db.session import get_session
from app.deps import get_embedding_service, verify_internal_secret
from app.schemas.chat import EmbedDocumentRequest, EmbedDocumentResponse

router = APIRouter(tags=["embed"], dependencies=[Depends(verify_internal_secret)])
log = structlog.get_logger(__name__)


@router.post("/embed/document", response_model=EmbedDocumentResponse)
async def embed_document(
    req: EmbedDocumentRequest,
    session: AsyncSession = Depends(get_session),
    embedder: EmbeddingService = Depends(get_embedding_service),
) -> EmbedDocumentResponse:
    repo = RagRepo(session)
    doc_id, n = await ingest_document(
        repo, embedder,
        source=req.source, title=req.title, content=req.content,
        lang=req.lang, quality=req.quality, extra_metadata=req.metadata,
    )
    await session.commit()
    return EmbedDocumentResponse(document_id=doc_id, chunks=n)


@router.post("/embed/user/{user_id}", status_code=202)
async def embed_user(user_id: str) -> dict:
    """NestJS gọi fire-and-forget khi dữ liệu user đổi. P1: no-op (chưa embed user data).
    P2/P3 sẽ cập nhật memory/summary qua Kafka thay vì embed dữ liệu cá nhân."""
    log.info("embed_user_trigger", has_user=bool(user_id))
    return {"status": "accepted"}
