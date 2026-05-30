"""POST /chat — khớp contract NestJS ChatbotService (đọc `reply`)."""
from __future__ import annotations

import json
import time

import structlog
from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sse_starlette.sse import EventSourceResponse

from app.core.llm_service import LLMService
from app.core.rag_orchestrator import RagOrchestrator
from app.deps import get_llm_service, get_orchestrator, verify_internal_secret
from app.db.session import get_session
from app.observability import metrics
from app.schemas.chat import ChatRequest, ChatResponse

router = APIRouter(tags=["chat"], dependencies=[Depends(verify_internal_secret)])
log = structlog.get_logger(__name__)


@router.post("/chat", response_model=ChatResponse)
async def chat(
    req: ChatRequest,
    orchestrator: RagOrchestrator = Depends(get_orchestrator),
    session: AsyncSession = Depends(get_session),
) -> ChatResponse:
    started = time.perf_counter()
    try:
        resp = await orchestrator.answer(req)
        await session.commit()
    except Exception:
        await session.rollback()
        metrics.CHAT_REQUESTS.labels(intent="unknown", status="error").inc()
        raise

    # metrics
    metrics.CHAT_REQUESTS.labels(intent=resp.intent, status="ok").inc()
    metrics.CHAT_LATENCY.labels(intent=resp.intent).observe(time.perf_counter() - started)
    if resp.usage.model:
        metrics.LLM_TOKENS.labels(model=resp.usage.model, direction="in").inc(resp.usage.tokens_in)
        metrics.LLM_TOKENS.labels(model=resp.usage.model, direction="out").inc(resp.usage.tokens_out)
        metrics.LLM_COST.labels(model=resp.usage.model).inc(resp.usage.cost_usd)
    metrics.RETRIEVAL_HITS.labels(intent=resp.intent).observe(len(resp.sources))
    metrics.SAFETY_EVENTS.labels(result=resp.safety.category or "ok").inc()
    return resp


@router.post("/chat/stream")
async def chat_stream(
    req: ChatRequest,
    orchestrator: RagOrchestrator = Depends(get_orchestrator),
    session: AsyncSession = Depends(get_session),
    llm: LLMService = Depends(get_llm_service),
):
    """SSE stream token. Các bước RAG đồng bộ chạy trước (trong scope session),
    sau đó stream LLM token. Sự kiện: meta → delta* → done."""
    plan = await orchestrator.prepare_stream(req)
    await session.commit()

    async def event_gen():
        if plan.blocked_reply:
            yield {"event": "delta", "data": json.dumps({"text": plan.blocked_reply}, ensure_ascii=False)}
            yield {"event": "done", "data": json.dumps({"intent": plan.intent, "sources": [], "disclaimer": None}, ensure_ascii=False)}
            return
        meta = {"intent": plan.intent, "sources": [s.model_dump() for s in plan.sources]}
        yield {"event": "meta", "data": json.dumps(meta, ensure_ascii=False)}
        async for delta in llm.stream(plan.messages):
            yield {"event": "delta", "data": json.dumps({"text": delta}, ensure_ascii=False)}
        if plan.disclaimer:
            yield {"event": "delta", "data": json.dumps({"text": f"\n\n> {plan.disclaimer}"}, ensure_ascii=False)}
        yield {"event": "done", "data": json.dumps({"intent": plan.intent, "disclaimer": plan.disclaimer}, ensure_ascii=False)}

    metrics.CHAT_REQUESTS.labels(intent=plan.intent, status="stream").inc()
    return EventSourceResponse(event_gen())
