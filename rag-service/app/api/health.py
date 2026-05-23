import httpx
from fastapi import APIRouter
from app.config import get_settings
from app.core.vector_store import get_vector_store

router = APIRouter(tags=["health"])


@router.get("/health")
async def health():
    settings = get_settings()
    return {
        "status": "ok",
        "llm_provider": settings.LLM_PROVIDER,
        "llm_model": settings.GEMINI_MODEL if settings.LLM_PROVIDER == "gemini" else settings.OLLAMA_LLM_MODEL,
        "embedding_provider": settings.EMBEDDING_PROVIDER,
    }


@router.get("/health/ready")
async def health_ready():
    settings = get_settings()

    # Only check Ollama if it's actually being used
    ollama_ok: bool | None = None
    if settings.LLM_PROVIDER == "ollama" or settings.EMBEDDING_PROVIDER == "ollama":
        try:
            async with httpx.AsyncClient(timeout=3.0) as client:
                resp = await client.get(f"{settings.OLLAMA_BASE_URL}/api/tags")
                ollama_ok = resp.status_code == 200
        except Exception:
            ollama_ok = False

    # Check knowledge base document count
    kb_docs = 0
    try:
        vs = get_vector_store()
        kb_docs = vs.count("knowledge_base")
    except Exception:
        pass

    # Ready = knowledge base populated (Ollama check only applies when used)
    ready = kb_docs > 0 and (ollama_ok is not False)

    result: dict = {"ready": ready, "knowledge_base_docs": kb_docs}
    if ollama_ok is not None:
        result["ollama_reachable"] = ollama_ok
    return result
