package com.vitalai.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_session")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val sessionName: String?,
    val sessionDate: String,
    val totalDurationMinutes: Int,
    val totalCaloriesBurned: Float,
    val detailsJson: String,
    val isPendingSync: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)
