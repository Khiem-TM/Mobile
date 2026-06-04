package com.vitalai.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_profile")
data class HealthProfileEntity(
    @PrimaryKey val id: String = "me",
    val birthDate: String?,
    val gender: String?,
    val heightCm: Float?,
    val initialWeightKg: Float?,
    val activityLevel: String?,
    val dietType: String?,
    val foodAllergies: String?,      // pipe-separated, e.g. "gluten|dairy"
    val caloriesGoal: Float?,
    val goalType: String?,
    val targetWeightKg: Float?,
    val dailyCaloriesGoal: Float?,
    val proteinGoalG: Float?,
    val fatGoalG: Float?,
    val carbsGoalG: Float?,
    val weeklyRateKg: Float?,
    val goalStartDate: String?,
    val goalDeadline: String?,
    val goalStatus: String?,
    val waterGoalMl: Int?,
    val stepGoal: Int?,
    val cachedAt: Long = System.currentTimeMillis()
)
