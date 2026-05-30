"""FastAPI RAG Core Service — entrypoint."""
from __future__ import annotations

from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI
from prometheus_client import CONTENT_TYPE_LATEST, generate_latest
from starlette.responses import Response

from app.api import chat, embed, health, reports
from app.config import get_settings
from app.core.embedding_service import EmbeddingService
from app.core.llm_service import LLMService
from app.observability.logging import configure_logging
from app.providers.factory import build_embedder, build_fallback_llm, build_llm

log = structlog.get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    s = get_settings()
    configure_logging(s.log_level)

    # Redis (cache embedding query) — optional
    redis = None
    try:
        import redis.asyncio as aioredis

        redis = aioredis.from_url(s.redis_url, decode_responses=True)
        await redis.ping()
        log.info("redis_connected")
    except Exception as e:
        log.warning("redis_unavailable", err=str(e))
        redis = None

    # Providers (load embed model 1 lần/worker)
    llm = build_llm()
    app.state.llm_service = LLMService(primary=llm, fallback=build_fallback_llm())
    embedder = build_embedder()
    app.state.embedding_service = EmbeddingService(embedder, redis=redis)
    app.state.redis = redis
    log.info("startup_complete", llm=llm.name, embedder=embedder.name, embed_dim=embedder.dim)

    yield

    if redis is not None:
        await redis.aclose()


def create_app() -> FastAPI:
    s = get_settings()
    app = FastAPI(title=s.app_name, version="0.1.0", lifespan=lifespan)

    app.include_router(health.router)
    app.include_router(chat.router)
    app.include_router(embed.router)
    app.include_router(reports.router)

    @app.get("/metrics")
    async def metrics() -> Response:
        return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)

    return app


app = create_app()
