import logging
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.vector_store import VectorStore
from app.db.queries import get_meal_logs_range
from app.utils.text_formatters import format_daily_meal_summary

logger = logging.getLogger(__name__)


async def embed_meal_history(user_id: str, db: AsyncSession, vs: VectorStore, days: int = 30) -> int:
    summaries = await get_meal_logs_range(db, user_id, days=days)
    if not summaries:
        logger.info(f"No meal data found for user {user_id}")
        return 0

    doc_ids = []
    texts = []
    metadatas = []

    for s in summaries:
        date_str = s["date"]
        doc_ids.append(f"meal_{user_id}_{date_str}")
        texts.append(format_daily_meal_summary(s))
        metadatas.append({
            "doc_type": "daily_meal_summary",
            "user_id": user_id,
            "date": date_str,
            "total_calories": float(s.get("total_calories", 0)),
            "total_protein": float(s.get("total_protein", 0)),
            "total_carbs": float(s.get("total_carbs", 0)),
            "total_fat": float(s.get("total_fat", 0)),
        })

    vs.upsert_batch("user_meal_history", doc_ids, texts, metadatas)
    logger.info(f"Meal history embedded for {user_id}: {len(summaries)} days")
    return len(summaries)
