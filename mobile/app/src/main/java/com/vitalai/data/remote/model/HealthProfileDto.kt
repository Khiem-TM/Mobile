package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthProfileDto(
    val birthDate: String? = null,
    val gender: String? = null,
    val heightCm: Float? = null,
    val initialWeightKg: Float? = null,
    val activityLevel: String? = null,
    val dietType: String? = null,
    val foodAllergies: List<String>? = null,
    val caloriesGoal: Float? = null,
    val goalType: String? = null,
    val targetWeightKg: Float? = null,
    val dailyCaloriesGoal: Float? = null,
    val proteinGoalG: Float? = null,
    val fatGoalG: Float? = null,
    val carbsGoalG: Float? = null,
    val weeklyRateKg: Float? = null,
    val goalStartDate: String? = null,
    val goalDeadline: String? = null,
    val goalStatus: String? = null,
    val waterGoalMl: Int? = null,
    val stepGoal: Int? = null
)

@JsonClass(generateAdapter = true)
data class OnboardingRequest(
    val birthDate: String,
    val gender: String,
    val heightCm: Float,
    val initialWeightKg: Float,
    val activityLevel: String,
    val goalType: String,
    val targetWeightKg: Float? = null,
    val dietType: String? = null,
    val foodAllergies: List<String>? = null
)
