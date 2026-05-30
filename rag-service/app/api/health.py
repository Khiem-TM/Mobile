"""Health & readiness."""
from __future__ import annotations

from fastapi import APIRouter, Request
from sqlalchemy import text

from app.config import get_settings
from app.db.session import engine

router = APIRouter(tags=["health"])


@router.get("/health")
async def health() -> dict:
    s = get_settings()
    return {"status": "ok", "service": s.app_name, "env": s.env}


@router.get("/ready")
async def ready(request: Request) -> dict:
    checks: dict[str, str] = {}
    try:
        async with engine.connect() as conn:
            await conn.execute(text("SELECT 1"))
        checks["db"] = "ok"
    except Exception as e:  # pragma: no cover
        checks["db"] = f"error: {e}"
    checks["llm"] = getattr(request.app.state, "llm_service", None) and request.app.state.llm_service.primary.name or "none"
    checks["embedder"] = getattr(request.app.state, "embedding_service", None) and request.app.state.embedding_service.provider.name or "none"
    ok = checks.get("db") == "ok"
    return {"status": "ready" if ok else "degraded", "checks": checks}
