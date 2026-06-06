package com.vitalai.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey val id: String,
    val weightKg: Float,
    val heightCm: Float?,
    val bodyFatPct: Float?,
    val waistCm: Float?,
    val hipCm: Float?,
    val chestCm: Float?,
    val neckCm: Float?,
    val armCm: Float?,
    val bmi: Float?,
    val bmr: Float?,
    val tdee: Float?,
    val notes: String?,
    val measuredAt: String,
    val cachedAt: Long = System.currentTimeMillis(),
    val isPendingSync: Boolean = false
)
