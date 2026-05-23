"""Read-only SQLAlchemy ORM models mirroring NestJS TypeORM entities."""
from datetime import datetime
from sqlalchemy import ARRAY, Column, Date, DateTime, Float, Integer, String, Text
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    pass


class UserHealthProfile(Base):
    __tablename__ = "user_health_profiles"

    id = Column(String, primary_key=True)
    userId = Column("user_id", String, nullable=False)
    birthDate = Column("birth_date", String, nullable=True)
    gender = Column(String, nullable=True)
    heightCm = Column("height_cm", Float, nullable=True)
    initialWeightKg = Column("initial_weight_kg", Float, nullable=True)
    activityLevel = Column("activity_level", String, nullable=True)
    dietType = Column("diet_type", String, nullable=True)
    goalType = Column("goal_type", String, nullable=True)
    targetWeightKg = Column("target_weight_kg", Float, nullable=True)
    dailyCaloriesGoal = Column("daily_calories_goal", Float, nullable=True)
    proteinGoalG = Column("protein_goal_g", Float, nullable=True)
    fatGoalG = Column("fat_goal_g", Float, nullable=True)
    carbsGoalG = Column("carbs_goal_g", Float, nullable=True)
    waterGoalMl = Column("water_goal_ml", Integer, nullable=True)
    weeklyRateKg = Column("weekly_rate_kg", Float, nullable=True)
    goalDeadline = Column("goal_deadline", String, nullable=True)
    goalStatus = Column("goal_status", String, nullable=True)
    foodAllergies = Column("food_allergies", ARRAY(String), nullable=True)


class BodyMetric(Base):
    __tablename__ = "body_metrics"

    id = Column(String, primary_key=True)
    userId = Column("user_id", String, nullable=False)
    recordedAt = Column("recorded_at", DateTime, nullable=True)
    weightKg = Column("weight_kg", Float, nullable=True)
    bodyFatPct = Column("body_fat_pct", Float, nullable=True)
    bmi = Column(Float, nullable=True)
    bmr = Column(Float, nullable=True)
    tdee = Column(Float, nullable=True)
    waistCm = Column("waist_cm", Float, nullable=True)
    hipCm = Column("hip_cm", Float, nullable=True)
    chestCm = Column("chest_cm", Float, nullable=True)
    neckCm = Column("neck_cm", Float, nullable=True)


class ActivityLog(Base):
    __tablename__ = "activity_logs"

    id = Column(String, primary_key=True)
    userId = Column("user_id", String, nullable=False)
    logDate = Column("log_date", String, nullable=False)
    steps = Column(Integer, default=0)
    caloriesBurned = Column("calories_burned", Float, default=0)
    activeMinutes = Column("active_minutes", Integer, default=0)
    waterMl = Column("water_ml", Integer, default=0)


class WorkoutSession(Base):
    __tablename__ = "workout_sessions"

    id = Column(String, primary_key=True)
    userId = Column("user_id", String, nullable=False)
    sessionDate = Column("session_date", String, nullable=False)
    sessionName = Column("session_name", String, nullable=True)
    totalDurationMinutes = Column("total_duration_minutes", Integer, default=0)
    totalCaloriesBurned = Column("total_calories_burned", Float, default=0)


class WorkoutSessionDetail(Base):
    __tablename__ = "workout_session_details"

    id = Column(String, primary_key=True)
    workoutSessionId = Column("workout_session_id", String, nullable=False)
    exerciseId = Column("exercise_id", String, nullable=False)
    orderIndex = Column("order_index", Integer, default=0)
    durationMinutes = Column("duration_minutes", Integer, default=0)
    weightKg = Column("weight_kg", Float, nullable=True)
    sets = Column(Integer, nullable=True)
    repsPerSet = Column("reps_per_set", Integer, nullable=True)
    caloriesBurned = Column("calories_burned", Float, default=0)


class Exercise(Base):
    __tablename__ = "exercises"

    id = Column(String, primary_key=True)
    name = Column(String, nullable=False)
    primaryMuscleGroup = Column("primary_muscle_group", String, nullable=True)
    intensity = Column(String, nullable=True)
    equipment = Column(String, nullable=True)
    metValue = Column("met_value", Float, default=0)


class MealLog(Base):
    __tablename__ = "meal_logs"

    id = Column(String, primary_key=True)
    userId = Column("user_id", String, nullable=False)
    logDate = Column("log_date", DateTime, nullable=False)
    mealType = Column("meal_type", String, nullable=False)


class MealLogItem(Base):
    __tablename__ = "meal_log_items"

    id = Column(String, primary_key=True)
    mealLogId = Column("meal_log_id", String, nullable=False)
    quantity = Column(Float, nullable=False)
    servingUnit = Column("serving_unit", String, nullable=True)
    caloriesSnapshot = Column("calories_snapshot", Float, nullable=True)
    proteinSnapshot = Column("protein_snapshot", Float, nullable=True)
    fatSnapshot = Column("fat_snapshot", Float, nullable=True)
    carbsSnapshot = Column("carbs_snapshot", Float, nullable=True)
    fiberSnapshot = Column("fiber_snapshot", Float, nullable=True)


class Food(Base):
    __tablename__ = "foods"

    id = Column(String, primary_key=True)
    name = Column(String, nullable=False)
    category = Column(String, nullable=True)
