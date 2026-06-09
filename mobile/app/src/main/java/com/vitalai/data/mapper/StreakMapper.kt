package com.vitalai.data.mapper

import com.vitalai.data.local.room.entity.StreakEntity
import com.vitalai.data.remote.model.StreakDto

fun StreakEntity.toDto() = StreakDto(
    loginStreak = loginStreak,
    mealLogStreak = mealLogStreak,
    workoutStreak = workoutStreak
)

fun StreakDto.toEntity() = StreakEntity(
    loginStreak = loginStreak,
    mealLogStreak = mealLogStreak,
    workoutStreak = workoutStreak
)
