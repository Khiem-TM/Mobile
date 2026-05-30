"""Dependency Injection cho FastAPI: auth nội bộ + dựng orchestrator theo request."""
from __future__ import annotations

from fastapi import Depends, Header, HTTPException, Request, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.core.embedding_service import EmbeddingService
from app.core.llm_service import LLMService
from app.core.rag_orchestrator import RagOrchestrator
from app.core.retriever import Retriever
from app.core.summary_service import SummaryService
from app.core.user_memory import UserMemory
from app.db.rag_repo import RagRepo
from app.db.session import get_session


async def verify_internal_secret(x_internal_secret: str | None = Header(default=None)) -> None:
    """Chỉ NestJS (giữ secret) được gọi. Khớp header `X-Internal-Secret` mà ChatbotService gửi."""
    expected = get_settings().nestjs_internal_secret
    if x_internal_secret != expected:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="invalid internal secret")


def get_llm_service(request: Request) -> LLMService:
    return request.app.state.llm_service


def get_embedding_service(request: Request) -> EmbeddingService:
    return request.app.state.embedding_service


async def get_orchestrator(
    session: AsyncSession = Depends(get_session),
    llm: LLMService = Depends(get_llm_service),
    embedder: EmbeddingService = Depends(get_embedding_service),
) -> RagOrchestrator:
    repo = RagRepo(session)
    retriever = Retriever(repo, embedder)
    memory = UserMemory(repo)
    summary = SummaryService(repo, llm)
    return RagOrchestrator(
        repo=repo, retriever=retriever, llm=llm, embedder=embedder, memory=memory, summary=summary
    )


async def get_summary_service(
    session: AsyncSession = Depends(get_session),
    llm: LLMService = Depends(get_llm_service),
) -> SummaryService:
    return SummaryService(RagRepo(session), llm)
