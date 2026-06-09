package com.vitalai.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_log")
data class ActivityLogEntity(
    @PrimaryKey val logDate: String,
    val serverId: String?,
    val steps: Int,
    val caloriesBurned: Float,
    val activeMinutes: Int,
    val waterMl: Int,
    val sleepHours: Float?,
    val mood: String?,
    val note: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
