import logging
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.vector_store import VectorStore
from app.db.queries import get_health_profile, get_latest_body_metric
from app.utils.text_formatters import format_user_profile_document

logger = logging.getLogger(__name__)


async def embed_user_profile(user_id: str, db: AsyncSession, vs: VectorStore) -> None:
    profile = await get_health_profile(db, user_id)
    metric = await get_latest_body_metric(db, user_id)

    if not profile and not metric:
        logger.info(f"No profile data found for user {user_id}, skipping embed")
        return

    doc_text = format_user_profile_document(profile, metric)
    metadata = {
        "doc_type": "user_profile",
        "user_id": user_id,
        "embedded_at": datetime.utcnow().isoformat(),
    }
    if metric:
        if metric.weightKg:
            metadata["weight_kg"] = float(metric.weightKg)
        if metric.bmi:
            metadata["bmi"] = float(metric.bmi)
    if profile:
        if profile.goalType:
            metadata["goal_type"] = profile.goalType
        if profile.dietType:
            metadata["diet_type"] = profile.dietType
        if profile.activityLevel:
            metadata["activity_level"] = profile.activityLevel

    vs.upsert(
        collection="user_profiles",
        doc_id=f"profile_{user_id}",
        text=doc_text,
        metadata=metadata,
    )
    logger.info(f"User profile embedded for {user_id}")
