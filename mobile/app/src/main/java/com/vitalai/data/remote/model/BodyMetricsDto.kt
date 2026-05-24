package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BodyMetricDto(
    @Json(name = "id") val id: String,
    @Json(name = "recordedAt") val date: String,
    @Json(name = "weightKg") val weightKg: Float,
    @Json(name = "bodyFatPct") val bodyFatPct: Float?,
    @Json(name = "waistCm") val waistCm: Float? = null,
    @Json(name = "hipCm") val hipCm: Float? = null,
    @Json(name = "chestCm") val chestCm: Float? = null,
    @Json(name = "neckCm") val neckCm: Float? = null,
    @Json(name = "bmi") val bmi: Float?,
    @Json(name = "bmr") val bmr: Float? = null,
    @Json(name = "tdee") val tdee: Float? = null,
    @Json(name = "notes") val notes: String?
)

@JsonClass(generateAdapter = true)
data class BodyMetricsPeriodDto(
    @Json(name = "data") val data: List<BodyMetricDto>,
    @Json(name = "avg_weight") val avgWeight: Float,
    @Json(name = "min_weight") val minWeight: Float,
    @Json(name = "max_weight") val maxWeight: Float
)

@JsonClass(generateAdapter = true)
data class UpsertBodyMetricRequest(
    @Json(name = "recordedAt") val recordedAt: String? = null,
    @Json(name = "weightKg") val weightKg: Float? = null,
    @Json(name = "bodyFatPct") val bodyFatPct: Float? = null,
    @Json(name = "waistCm") val waistCm: Float? = null,
    @Json(name = "hipCm") val hipCm: Float? = null,
    @Json(name = "chestCm") val chestCm: Float? = null,
    @Json(name = "neckCm") val neckCm: Float? = null,
    @Json(name = "notes") val notes: String? = null
)
