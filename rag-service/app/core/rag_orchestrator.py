"""Điều phối pipeline RAG (plan mục 5):
safety(pre) → intent → retrieve → memory → prompt → llm → safety(post) → log.
"""
from __future__ import annotations

import hashlib
import uuid
from dataclasses import dataclass, field

import structlog

from app.core import intent as intent_mod
from app.core import prompt_builder, safety_guard
from app.core.embedding_service import EmbeddingService
from app.core.llm_service import LLMService
from app.core.retriever import Retriever
from app.core.summary_service import SummaryService
from app.core.user_memory import UserMemory
from app.db.rag_repo import RagRepo
from app.providers.base import Message
from app.schemas.chat import ChatRequest, ChatResponse, Safety, Source, TokenUsage

log = structlog.get_logger(__name__)


def hash_user_ref(user_id: str) -> str:
    return "u_" + hashlib.sha256(user_id.encode()).hexdigest()[:24]


@dataclass
class StreamPlan:
    """Kết quả các bước đồng bộ trước khi stream token (an toàn với vòng đời session)."""
    blocked_reply: str | None = None
    intent: str = "smalltalk"
    messages: list = field(default_factory=list)
    sources: list = field(default_factory=list)
    disclaimer: str | None = None


class RagOrchestrator:
    def __init__(
        self,
        repo: RagRepo,
        retriever: Retriever,
        llm: LLMService,
        embedder: EmbeddingService,
        memory: UserMemory,
        summary: SummaryService,
    ) -> None:
        self.repo = repo
        self.retriever = retriever
        self.llm = llm
        self.embedder = embedder
        self.memory = memory
        self.summary = summary

    async def answer(self, req: ChatRequest) -> ChatResponse:
        request_id = req.request_id or str(uuid.uuid4())
        user_ref = hash_user_ref(req.user_id)
        allergies = ((req.user_context or {}).get("profile") or {}).get("allergies") or []

        # 1) Safety pre-check
        pre = safety_guard.check_input(req.message, allergies)
        if pre.blocked:
            log.warning("safety_block_input", request_id=request_id, category=pre.category)
            await self.repo.log_prompt(
                request_id=uuid.UUID(request_id), user_ref=user_ref, intent="crisis",
                prompt_version=prompt_builder.PROMPT_VERSION, model="safety",
                tokens_in=0, tokens_out=0, cost_usd=0, safety_result="blocked",
            )
            return ChatResponse(
                reply=pre.safe_reply or "",
                answer_markdown=pre.safe_reply or "",
                intent="mental_health",
                safety=Safety(flagged=True, category=pre.category, disclaimer=None),
                request_id=request_id,
            )

        # 2) Intent
        detected = intent_mod.classify(req.message)

        # 3) Retrieve
        chunks, retr_latency = await self.retriever.retrieve(detected, req.message)

        # 4) Memory
        recent_summary = await self.memory.get_summary(user_ref)

        # 5) Prompt + LLM
        history = [Message(role=t.role, content=t.content) for t in req.conversation_history]
        messages = prompt_builder.build_messages(
            intent=detected,
            message=req.message,
            user_context=req.user_context,
            recent_summary=recent_summary,
            chunks=chunks,
            history=history,
        )
        result = await self.llm.generate(messages)
        text = result.text

        # 6) Safety post-check
        post = safety_guard.check_output(text, detected)
        if post.disclaimer:
            text = f"{text}\n\n> {post.disclaimer}"

        # 6b) Rolling memory (best-effort, có điều kiện để tiết kiệm chi phí)
        if self.summary.should_refresh(len(history)):
            await self.summary.refresh_conversation_memory(
                user_ref, recent_summary, history, req.message, result.text
            )

        # 7) Logs
        await self.repo.log_retrieval(
            request_id=uuid.UUID(request_id), user_ref=user_ref, intent=detected,
            query=req.message, chunk_ids=[c.id for c in chunks],
            scores=[c.score for c in chunks], top_k=len(chunks), latency_ms=retr_latency,
        )
        await self.repo.log_prompt(
            request_id=uuid.UUID(request_id), user_ref=user_ref, intent=detected,
            prompt_version=prompt_builder.PROMPT_VERSION, model=result.model,
            tokens_in=result.tokens_in, tokens_out=result.tokens_out, cost_usd=result.cost_usd,
            safety_result=post.category or "ok",
        )

        sources = [
            Source(title=c.title, document_id=str(c.document_id), chunk_index=c.chunk_index)
            for c in chunks
        ]
        return ChatResponse(
            reply=text,
            answer_markdown=text,
            intent=detected,
            sources=sources,
            safety=Safety(flagged=bool(post.category), category=post.category, disclaimer=post.disclaimer),
            usage=TokenUsage(
                tokens_in=result.tokens_in, tokens_out=result.tokens_out,
                cost_usd=result.cost_usd, model=result.model,
            ),
            request_id=request_id,
        )

    async def prepare_stream(self, req: ChatRequest) -> StreamPlan:
        """Chạy các bước đồng bộ (safety/intent/retrieve/prompt) + ghi retrieval log.
        Token sẽ được stream bởi caller. Trả về StreamPlan."""
        request_id = req.request_id or str(uuid.uuid4())
        user_ref = hash_user_ref(req.user_id)
        allergies = ((req.user_context or {}).get("profile") or {}).get("allergies") or []

        pre = safety_guard.check_input(req.message, allergies)
        if pre.blocked:
            return StreamPlan(blocked_reply=pre.safe_reply or "", intent="mental_health")

        detected = intent_mod.classify(req.message)
        chunks, retr_latency = await self.retriever.retrieve(detected, req.message)
        recent_summary = await self.memory.get_summary(user_ref)
        history = [Message(role=t.role, content=t.content) for t in req.conversation_history]
        messages = prompt_builder.build_messages(
            intent=detected, message=req.message, user_context=req.user_context,
            recent_summary=recent_summary, chunks=chunks, history=history,
        )
        await self.repo.log_retrieval(
            request_id=uuid.UUID(request_id), user_ref=user_ref, intent=detected,
            query=req.message, chunk_ids=[c.id for c in chunks],
            scores=[c.score for c in chunks], top_k=len(chunks), latency_ms=retr_latency,
        )
        disclaimer = safety_guard.check_output("", detected).disclaimer
        sources = [
            Source(title=c.title, document_id=str(c.document_id), chunk_index=c.chunk_index)
            for c in chunks
        ]
        return StreamPlan(intent=detected, messages=messages, sources=sources, disclaimer=disclaimer)
