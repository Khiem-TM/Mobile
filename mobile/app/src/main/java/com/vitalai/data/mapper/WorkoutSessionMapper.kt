package com.vitalai.data.mapper

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vitalai.data.local.room.entity.WorkoutSessionEntity
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto

private val detailsType = Types.newParameterizedType(List::class.java, SessionExerciseDto::class.java)

fun WorkoutSessionEntity.toDto(moshi: Moshi): WorkoutSessionDto {
    val adapter = moshi.adapter<List<SessionExerciseDto>>(detailsType)
    val details = runCatching { adapter.fromJson(detailsJson) }.getOrNull() ?: emptyList()
    return WorkoutSessionDto(
        id = id,
        sessionName = sessionName,
        sessionDate = sessionDate,
        totalDurationMinutes = totalDurationMinutes,
        totalCaloriesBurned = totalCaloriesBurned,
        details = details
    )
}

fun WorkoutSessionDto.toEntity(
    moshi: Moshi,
    isPendingSync: Boolean = false
): WorkoutSessionEntity {
    val adapter = moshi.adapter<List<SessionExerciseDto>>(detailsType)
    return WorkoutSessionEntity(
        id = id,
        sessionName = sessionName,
        sessionDate = sessionDate,
        totalDurationMinutes = totalDurationMinutes,
        totalCaloriesBurned = totalCaloriesBurned,
        detailsJson = adapter.toJson(details),
        isPendingSync = isPendingSync
    )
}
