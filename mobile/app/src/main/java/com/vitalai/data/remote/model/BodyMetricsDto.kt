package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BodyMetricDto(
    @Json(name = "id") val id: String,
    @Json(name = "date") val date: String,
    @Json(name = "weight_kg") val weightKg: Float,
    @Json(name = "body_fat_pct") val bodyFatPct: Float?,
    @Json(name = "muscle_mass_kg") val muscleMassKg: Float?,
    @Json(name = "bmi") val bmi: Float?,
    @Json(name = "notes") val notes: String?
)

@JsonClass(generateAdapter = true)
data class BodyMetricsPeriodDto(
    @Json(name = "data") val data: List<BodyMetricDto>,
    @Json(name = "avg_weight") val avgWeight: Float,
    @Json(name = "min_weight") val minWeight: Float,
    @Json(name = "max_weight") val maxWeight: Float
)
