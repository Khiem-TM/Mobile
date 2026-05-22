from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Request
from app.config import Settings, get_settings
from app.schemas.embed import EmbedUserResponse, EmbedKnowledgeResponse

router = APIRouter(tags=["embed"])


def _verify_internal_secret(request: Request, settings: Settings) -> None:
    secret = request.headers.get("X-Internal-Secret", "")
    if secret != settings.NESTJS_INTERNAL_SECRET:
        raise HTTPException(status_code=401, detail="Unauthorized")


async def _embed_user_task(user_id: str, settings: Settings) -> None:
    """Background task: fetch user data from DB and re-embed into vector store."""
    import logging
    from app.core.vector_store import get_vector_store
    from app.db.connection import get_session_factory
    from app.embeddings.user_profile import embed_user_profile
    from app.embeddings.meal_history import embed_meal_history
    from app.embeddings.workout_history import embed_workout_history

    logger = logging.getLogger(__name__)
    try:
        vs = get_vector_store()
        session_factory = get_session_factory()
        async with session_factory() as db:
            await embed_user_profile(user_id, db, vs)
            await embed_meal_history(user_id, db, vs)
            await embed_workout_history(user_id, db, vs)
        logger.info(f"User {user_id} fully embedded")
    except Exception as e:
        logger.error(f"Failed to embed user {user_id}: {e}")


@router.post("/embed/user/{user_id}", response_model=EmbedUserResponse, status_code=202)
async def embed_user(
    user_id: str,
    request: Request,
    background_tasks: BackgroundTasks,
    settings: Settings = Depends(get_settings),
):
    _verify_internal_secret(request, settings)
    background_tasks.add_task(_embed_user_task, user_id, settings)
    return EmbedUserResponse(
        status="accepted",
        user_id=user_id,
        message="User embedding queued",
    )


@router.post("/embed/knowledge", response_model=EmbedKnowledgeResponse)
async def embed_knowledge(
    request: Request,
    settings: Settings = Depends(get_settings),
):
    _verify_internal_secret(request, settings)
    from app.core.vector_store import get_vector_store
    from app.embeddings.knowledge_base import embed_knowledge_base
    vs = get_vector_store()
    count = embed_knowledge_base(vs)
    return EmbedKnowledgeResponse(status="ok", documents_embedded=count)
