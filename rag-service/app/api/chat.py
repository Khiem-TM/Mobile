from fastapi import APIRouter, Depends, HTTPException, Request
from app.config import Settings, get_settings
from app.core.vector_store import get_vector_store
from app.rag.pipeline import run_rag_pipeline
from app.schemas.chat import ChatRequest, ChatResponse

router = APIRouter(tags=["chat"])


def _verify_internal_secret(request: Request, settings: Settings) -> None:
    secret = request.headers.get("X-Internal-Secret", "")
    if secret != settings.NESTJS_INTERNAL_SECRET:
        raise HTTPException(status_code=401, detail="Unauthorized")


@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: Request,
    body: ChatRequest,
    settings: Settings = Depends(get_settings),
):
    _verify_internal_secret(request, settings)
    vs = get_vector_store()
    return await run_rag_pipeline(body, vs, settings)
