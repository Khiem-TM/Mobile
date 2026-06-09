package com.vitalai.data.mapper

import com.vitalai.data.local.room.entity.ActivityLogEntity
import com.vitalai.data.remote.model.ActivityLogDto

fun ActivityLogEntity.toDto(): ActivityLogDto =
    ActivityLogDto(
        id = serverId,
        logDate = logDate,
        steps = steps,
        caloriesBurned = caloriesBurned,
        activeMinutes = activeMinutes,
        waterMl = waterMl,
        sleepHours = sleepHours,
        mood = mood,
        note = note
    )

fun ActivityLogDto.toEntity(): ActivityLogEntity =
    ActivityLogEntity(
        logDate = logDate,
        serverId = id,
        steps = steps,
        caloriesBurned = caloriesBurned,
        activeMinutes = activeMinutes,
        waterMl = waterMl,
        sleepHours = sleepHours,
        mood = mood,
        note = note
    )
