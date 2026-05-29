package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BodyMetricDto(
    @Json(name = "id") val id: String,
    @Json(name = "measuredAt") val date: String,
    @Json(name = "weightKg") val weightKg: Float,
    @Json(name = "heightCm") val heightCm: Float? = null,
    @Json(name = "bodyFatPercent") val bodyFatPct: Float? = null,
    @Json(name = "waistCm") val waistCm: Float? = null,
    @Json(name = "hipCm") val hipCm: Float? = null,
    @Json(name = "chestCm") val chestCm: Float? = null,
    @Json(name = "neckCm") val neckCm: Float? = null,
    @Json(name = "armCm") val armCm: Float? = null,
    @Json(name = "bmi") val bmi: Float? = null,
    @Json(name = "bmr") val bmr: Float? = null,
    @Json(name = "tdee") val tdee: Float? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class BodyMetricsPeriodDto(
    @Json(name = "data") val data: List<BodyMetricDto>,
    @Json(name = "avg_weight") val avgWeight: Float,
    @Json(name = "min_weight") val minWeight: Float,
    @Json(name = "max_weight") val maxWeight: Float
)

@JsonClass(generateAdapter = true)
data class BodyMetricsSummaryDto(
    @Json(name = "startWeight") val startWeight: Float = 0f,
    @Json(name = "currentWeight") val currentWeight: Float = 0f,
    @Json(name = "weightChange") val weightChange: Float = 0f,
    @Json(name = "startDate") val startDate: String? = null,
    @Json(name = "latestDate") val latestDate: String? = null,
    @Json(name = "totalRecords") val totalRecords: Int = 0
)

@JsonClass(generateAdapter = true)
data class ProgressPhotoDto(
    @Json(name = "id") val id: String,
    @Json(name = "photoUrl") val photoUrl: String,
    @Json(name = "photoType") val photoType: String,
    @Json(name = "takenAt") val takenAt: String? = null,
    @Json(name = "bodyMetricId") val bodyMetricId: String? = null
)

@JsonClass(generateAdapter = true)
data class UpsertBodyMetricRequest(
    @Json(name = "measuredAt") val recordedAt: String? = null,
    @Json(name = "weightKg") val weightKg: Float? = null,
    @Json(name = "heightCm") val heightCm: Float? = null,
    @Json(name = "bodyFatPercent") val bodyFatPct: Float? = null,
    @Json(name = "waistCm") val waistCm: Float? = null,
    @Json(name = "hipCm") val hipCm: Float? = null,
    @Json(name = "chestCm") val chestCm: Float? = null,
    @Json(name = "neckCm") val neckCm: Float? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class BasicBodyMetricRequest(
    @Json(name = "measuredAt") val measuredAt: String? = null,
    @Json(name = "weightKg") val weightKg: Float,
    @Json(name = "heightCm") val heightCm: Float
)

@JsonClass(generateAdapter = true)
data class AdvancedBodyMetricRequest(
    @Json(name = "measuredAt") val measuredAt: String? = null,
    @Json(name = "bodyFatPercent") val bodyFatPct: Float? = null,
    @Json(name = "waistCm") val waistCm: Float? = null,
    @Json(name = "hipCm") val hipCm: Float? = null,
    @Json(name = "chestCm") val chestCm: Float? = null,
    @Json(name = "neckCm") val neckCm: Float? = null,
    @Json(name = "armCm") val armCm: Float? = null,
    @Json(name = "notes") val notes: String? = null
)
