package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardDto(
    @Json(name = "calorie_goal") val calorieGoal: Int,
    @Json(name = "calories_consumed") val caloriesConsumed: Float,
    @Json(name = "calories_burned") val caloriesBurned: Float,
    @Json(name = "carbs_g") val carbsG: Float,
    @Json(name = "carbs_goal") val carbsGoal: Float,
    @Json(name = "protein_g") val proteinG: Float,
    @Json(name = "protein_goal") val proteinGoal: Float,
    @Json(name = "fat_g") val fatG: Float,
    @Json(name = "fat_goal") val fatGoal: Float,
    @Json(name = "water_ml") val waterMl: Int,
    @Json(name = "water_goal_ml") val waterGoalMl: Int,
    @Json(name = "steps") val steps: Int
)

@JsonClass(generateAdapter = true)
data class DashboardPeriodDto(
    @Json(name = "from") val from: String,
    @Json(name = "to") val to: String,
    @Json(name = "year") val year: Int? = null,
    @Json(name = "month") val month: Int? = null
)

@JsonClass(generateAdapter = true)
data class DashboardNutritionReportDto(
    @Json(name = "avg_daily_calories") val avgDailyCalories: Int = 0,
    @Json(name = "total_calories") val totalCalories: Int = 0,
    @Json(name = "days_logged") val daysLogged: Int = 0
)

@JsonClass(generateAdapter = true)
data class DashboardActivityReportDto(
    @Json(name = "total_steps") val totalSteps: Int = 0,
    @Json(name = "avg_daily_steps") val avgDailySteps: Int = 0,
    @Json(name = "total_water_ml") val totalWaterMl: Int = 0,
    @Json(name = "total_active_minutes") val totalActiveMinutes: Int = 0,
    @Json(name = "active_days") val activeDays: Int = 0
)

@JsonClass(generateAdapter = true)
data class DashboardTrainingReportDto(
    @Json(name = "session_count") val workoutCount: Int = 0,
    @Json(name = "avg_sessions_per_week") val avgWorkoutsPerWeek: Float? = null
)
@JsonClass(generateAdapter = true)
data class DashboardWeeklyDto(
    @Json(name = "period") val period: DashboardPeriodDto,
    @Json(name = "nutrition") val nutrition: DashboardNutritionReportDto,
    @Json(name = "activity") val activity: DashboardActivityReportDto,
    @Json(name = "training") val training: DashboardTrainingReportDto
)

@JsonClass(generateAdapter = true)
data class DashboardMonthlyBodyDto(
    @Json(name = "weight_change_kg") val weightChangeKg: Float? = null
)

@JsonClass(generateAdapter = true)
data class DashboardMonthlyDto(
    @Json(name = "period") val period: DashboardPeriodDto,
    @Json(name = "nutrition") val nutrition: DashboardNutritionReportDto,
    @Json(name = "activity") val activity: DashboardActivityReportDto,
    @Json(name = "training") val training: DashboardTrainingReportDto,
    @Json(name = "body") val body: DashboardMonthlyBodyDto? = null
)

@JsonClass(generateAdapter = true)
data class StreakDto(
    @Json(name = "login_streak") val loginStreak: Int,
    @Json(name = "meal_log_streak") val mealLogStreak: Int,
    @Json(name = "workout_streak") val workoutStreak: Int
)

@JsonClass(generateAdapter = true)
data class UnreadCountDto(
    @Json(name = "count") val count: Int
)

@JsonClass(generateAdapter = true)
data class DailyDashboardResponse(
    @Json(name = "date") val date: String,
    @Json(name = "nutrition") val nutrition: DailyNutritionDto,
    @Json(name = "activity") val activity: DailyActivityDto,
    @Json(name = "body") val body: DailyBodyDto,
    @Json(name = "streaks") val streaks: StreakDto? = null
)

@JsonClass(generateAdapter = true)
data class DailyNutritionDto(
    @Json(name = "total_calories") val totalCalories: Float,
    @Json(name = "total_protein") val totalProtein: Float,
    @Json(name = "total_fat") val totalFat: Float,
    @Json(name = "total_carbs") val totalCarbs: Float,
    @Json(name = "total_fiber") val totalFiber: Float = 0f
)

@JsonClass(generateAdapter = true)
data class DailyActivityDto(
    @Json(name = "steps") val steps: Int = 0,
    @Json(name = "water_ml") val waterMl: Int = 0,
    @Json(name = "sleep_hours") val sleepHours: Float? = null,
    @Json(name = "mood") val mood: String? = null
)

@JsonClass(generateAdapter = true)
data class DailyBodyDto(
    @Json(name = "current_weight") val currentWeight: Float? = null,
    @Json(name = "bmi") val bmi: Float? = null
)

