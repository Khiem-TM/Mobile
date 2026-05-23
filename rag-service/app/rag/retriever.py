import asyncio
import logging
from app.core.vector_store import VectorStore

logger = logging.getLogger(__name__)


async def retrieve_context(vs: VectorStore, user_id: str, query: str) -> dict[str, list[str]]:
    """Run parallel retrieval from all 4 collections."""

    def _query_sync(collection: str, n: int, where: dict | None = None) -> list[str]:
        return vs.query(collection, query, n_results=n, where=where)

    loop = asyncio.get_event_loop()
    user_filter = {"user_id": user_id}

    profile_docs, meal_docs, workout_docs, kb_docs = await asyncio.gather(
        loop.run_in_executor(None, lambda: _query_sync("user_profiles", 1, user_filter)),
        loop.run_in_executor(None, lambda: _query_sync("user_meal_history", 5, user_filter)),
        loop.run_in_executor(None, lambda: _query_sync("user_workout_history", 3, user_filter)),
        loop.run_in_executor(None, lambda: _query_sync("knowledge_base", 5, None)),
    )

    logger.debug(
        f"Retrieved: profile={len(profile_docs)}, meal={len(meal_docs)}, "
        f"workout={len(workout_docs)}, kb={len(kb_docs)}"
    )

    return {
        "profile": profile_docs,
        "meals": meal_docs,
        "workouts": workout_docs,
        "knowledge": kb_docs,
    }


def count_sources(context: dict[str, list[str]]) -> int:
    return sum(len(v) for v in context.values())
