package com.vitalai.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val id: String = "me",
    val loginStreak: Int,
    val mealLogStreak: Int,
    val workoutStreak: Int,
    val cachedAt: Long = System.currentTimeMillis()
)
