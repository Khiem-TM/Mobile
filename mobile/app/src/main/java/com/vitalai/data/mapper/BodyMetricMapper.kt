package com.vitalai.data.mapper

import com.vitalai.data.local.room.entity.BodyMetricEntity
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import java.time.LocalDate

fun BodyMetricEntity.toDto() = BodyMetricDto(
    id = id,
    date = measuredAt,
    weightKg = weightKg,
    heightCm = heightCm,
    bodyFatPct = bodyFatPct,
    waistCm = waistCm,
    hipCm = hipCm,
    chestCm = chestCm,
    neckCm = neckCm,
    armCm = armCm,
    bmi = bmi,
    bmr = bmr,
    tdee = tdee,
    notes = notes
)

fun BodyMetricDto.toEntity(isPendingSync: Boolean = false) = BodyMetricEntity(
    id = id,
    weightKg = weightKg,
    heightCm = heightCm,
    bodyFatPct = bodyFatPct,
    waistCm = waistCm,
    hipCm = hipCm,
    chestCm = chestCm,
    neckCm = neckCm,
    armCm = armCm,
    bmi = bmi,
    bmr = bmr,
    tdee = tdee,
    notes = notes,
    measuredAt = date,
    isPendingSync = isPendingSync
)

fun UpsertBodyMetricRequest.toEntity(id: String, isPendingSync: Boolean = true) = BodyMetricEntity(
    id = id,
    weightKg = weightKg ?: 0f,
    heightCm = heightCm,
    bodyFatPct = bodyFatPct,
    waistCm = waistCm,
    hipCm = hipCm,
    chestCm = chestCm,
    neckCm = neckCm,
    armCm = armCm,
    bmi = null,
    bmr = null,
    tdee = null,
    notes = notes,
    measuredAt = recordedAt ?: LocalDate.now().toString(),
    isPendingSync = isPendingSync
)

fun UpsertBodyMetricRequest.toDto(tempId: String) = BodyMetricDto(
    id = tempId,
    date = recordedAt ?: LocalDate.now().toString(),
    weightKg = weightKg ?: 0f,
    heightCm = heightCm,
    bodyFatPct = bodyFatPct,
    waistCm = waistCm,
    hipCm = hipCm,
    chestCm = chestCm,
    neckCm = neckCm,
    armCm = armCm,
    bmi = null,
    bmr = null,
    tdee = null,
    notes = notes
)

fun String.toDateRange(): Pair<String, String> {
    val today = LocalDate.now()
    val from = when (this) {
        "week" -> today.minusDays(6)
        "month" -> today.minusDays(29)
        "3months" -> today.minusDays(89)
        else -> today.minusDays(6)
    }
    return Pair(from.toString(), today.toString())
}
