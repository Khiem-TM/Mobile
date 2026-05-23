import logging
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.vector_store import VectorStore
from app.db.queries import get_workout_sessions_range
from app.utils.text_formatters import format_workout_session

logger = logging.getLogger(__name__)


async def embed_workout_history(user_id: str, db: AsyncSession, vs: VectorStore, days: int = 30) -> int:
    sessions = await get_workout_sessions_range(db, user_id, days=days)
    if not sessions:
        logger.info(f"No workout data found for user {user_id}")
        return 0

    doc_ids = []
    texts = []
    metadatas = []

    for s in sessions:
        session_id = s["session_id"]
        doc_ids.append(f"workout_{session_id}")
        texts.append(format_workout_session(s))
        metadatas.append({
            "doc_type": "workout_session",
            "user_id": user_id,
            "session_date": s.get("date", ""),
            "total_duration_minutes": int(s.get("total_duration_minutes", 0)),
            "total_calories_burned": float(s.get("total_calories_burned", 0)),
            "primary_muscle_groups": ",".join(s.get("muscle_groups", [])),
        })

    vs.upsert_batch("user_workout_history", doc_ids, texts, metadatas)
    logger.info(f"Workout history embedded for {user_id}: {len(sessions)} sessions")
    return len(sessions)
