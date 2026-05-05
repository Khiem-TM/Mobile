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
    val goalType: String? = null,
    val targetWeightKg: Float? = null,
    val dailyCaloriesGoal: Int? = null,
    val waterGoalMl: Int? = null
)
