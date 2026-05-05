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
data class StreakDto(
    @Json(name = "login_streak") val loginStreak: Int,
    @Json(name = "meal_log_streak") val mealLogStreak: Int,
    @Json(name = "workout_streak") val workoutStreak: Int
)

@JsonClass(generateAdapter = true)
data class UnreadCountDto(
    @Json(name = "count") val count: Int
)
