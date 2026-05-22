"""Read-only query functions for RAG data access."""
from datetime import datetime, timedelta
from sqlalchemy import select, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import (
    UserHealthProfile,
    BodyMetric,
    ActivityLog,
    WorkoutSession,
    WorkoutSessionDetail,
    Exercise,
    MealLog,
    MealLogItem,
    Food,
)


async def get_health_profile(db: AsyncSession, user_id: str) -> UserHealthProfile | None:
    result = await db.execute(
        select(UserHealthProfile).where(UserHealthProfile.userId == user_id)
    )
    return result.scalar_one_or_none()


async def get_latest_body_metric(db: AsyncSession, user_id: str) -> BodyMetric | None:
    result = await db.execute(
        select(BodyMetric)
        .where(BodyMetric.userId == user_id)
        .order_by(BodyMetric.recordedAt.desc())
        .limit(1)
    )
    return result.scalar_one_or_none()


async def get_meal_logs_range(
    db: AsyncSession, user_id: str, days: int = 30
) -> list[dict]:
    since = (datetime.utcnow() - timedelta(days=days)).date()
    result = await db.execute(
        select(MealLog).where(
            and_(
                MealLog.userId == user_id,
                MealLog.logDate >= since,
            )
        ).order_by(MealLog.logDate.desc())
    )
    logs = result.scalars().all()
    if not logs:
        return []

    log_ids = [log.id for log in logs]
    items_result = await db.execute(
        select(MealLogItem).where(MealLogItem.mealLogId.in_(log_ids))
    )
    items = items_result.scalars().all()

    items_by_log: dict[str, list[MealLogItem]] = {}
    for item in items:
        items_by_log.setdefault(item.mealLogId, []).append(item)

    summaries: dict[str, dict] = {}
    for log in logs:
        date_str = log.logDate.strftime("%Y-%m-%d") if hasattr(log.logDate, "strftime") else str(log.logDate)[:10]
        if date_str not in summaries:
            summaries[date_str] = {
                "date": date_str,
                "meals": [],
                "total_calories": 0.0,
                "total_protein": 0.0,
                "total_carbs": 0.0,
                "total_fat": 0.0,
                "total_fiber": 0.0,
            }
        log_items = items_by_log.get(log.id, [])
        meal_cals = sum(i.caloriesSnapshot or 0 for i in log_items)
        summaries[date_str]["meals"].append({
            "type": log.mealType,
            "calories": round(meal_cals, 1),
        })
        summaries[date_str]["total_calories"] += meal_cals
        summaries[date_str]["total_protein"] += sum(i.proteinSnapshot or 0 for i in log_items)
        summaries[date_str]["total_carbs"] += sum(i.carbsSnapshot or 0 for i in log_items)
        summaries[date_str]["total_fat"] += sum(i.fatSnapshot or 0 for i in log_items)
        summaries[date_str]["total_fiber"] += sum(i.fiberSnapshot or 0 for i in log_items)

    for s in summaries.values():
        for key in ("total_calories", "total_protein", "total_carbs", "total_fat", "total_fiber"):
            s[key] = round(s[key], 1)

    return sorted(summaries.values(), key=lambda x: x["date"], reverse=True)


async def get_workout_sessions_range(
    db: AsyncSession, user_id: str, days: int = 30
) -> list[dict]:
    since = (datetime.utcnow() - timedelta(days=days)).date().isoformat()
    result = await db.execute(
        select(WorkoutSession)
        .where(
            and_(
                WorkoutSession.userId == user_id,
                WorkoutSession.sessionDate >= since,
            )
        )
        .order_by(WorkoutSession.sessionDate.desc())
    )
    sessions = result.scalars().all()
    if not sessions:
        return []

    session_ids = [s.id for s in sessions]
    details_result = await db.execute(
        select(WorkoutSessionDetail)
        .where(WorkoutSessionDetail.workoutSessionId.in_(session_ids))
    )
    details = details_result.scalars().all()

    exercise_ids = list({d.exerciseId for d in details})
    exercises_by_id: dict[str, Exercise] = {}
    if exercise_ids:
        ex_result = await db.execute(
            select(Exercise).where(Exercise.id.in_(exercise_ids))
        )
        exercises_by_id = {e.id: e for e in ex_result.scalars().all()}

    details_by_session: dict[str, list[WorkoutSessionDetail]] = {}
    for d in details:
        details_by_session.setdefault(d.workoutSessionId, []).append(d)

    results = []
    for session in sessions:
        session_details = details_by_session.get(session.id, [])
        exercises_info = []
        muscle_groups = set()
        for d in session_details:
            ex = exercises_by_id.get(d.exerciseId)
            if ex:
                if ex.primaryMuscleGroup:
                    muscle_groups.add(ex.primaryMuscleGroup)
                exercises_info.append({
                    "name": ex.name,
                    "muscle_group": ex.primaryMuscleGroup,
                    "intensity": ex.intensity,
                    "sets": d.sets,
                    "reps_per_set": d.repsPerSet,
                    "weight_kg": d.weightKg,
                    "duration_minutes": d.durationMinutes,
                    "calories_burned": d.caloriesBurned,
                })
        results.append({
            "session_id": session.id,
            "date": session.sessionDate,
            "session_name": session.sessionName,
            "total_duration_minutes": session.totalDurationMinutes,
            "total_calories_burned": session.totalCaloriesBurned,
            "exercises": exercises_info,
            "muscle_groups": list(muscle_groups),
        })
    return results
