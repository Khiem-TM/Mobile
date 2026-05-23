package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExerciseDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "primaryMuscleGroup") val primaryMuscleGroup: String = "",
    @Json(name = "equipment") val equipment: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "instructions") val instructions: String? = null,
    @Json(name = "formTips") val formTips: String? = null,
    @Json(name = "intensity") val intensity: String? = null,
    @Json(name = "imageAvtUrl") val imageAvtUrl: String? = null,
    @Json(name = "imageUrl") val imageUrl: List<String>? = null,
    @Json(name = "metValue") val metValue: Float = 0f,
    @Json(name = "caloriesPerMin") val caloriesPerMin: Float = 0f
) {
    val displayImageUrl: String? get() = imageAvtUrl
    val muscleGroup: String get() = primaryMuscleGroup
}

@JsonClass(generateAdapter = true)
data class WorkoutSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "sessionName") val sessionName: String? = null,
    @Json(name = "sessionDate") val sessionDate: String,
    @Json(name = "totalDurationMinutes") val totalDurationMinutes: Int = 0,
    @Json(name = "totalCaloriesBurned") val totalCaloriesBurned: Float = 0f,
    @Json(name = "details") val details: List<SessionExerciseDto> = emptyList()
) {
    val name: String? get() = sessionName
    val date: String get() = sessionDate
    val durationMin: Int get() = totalDurationMinutes
    val totalCalories: Float get() = totalCaloriesBurned
    val exercises: List<SessionExerciseDto> get() = details
}

@JsonClass(generateAdapter = true)
data class SessionExerciseDto(
    @Json(name = "id") val id: String,
    @Json(name = "exerciseId") val exerciseId: String,
    @Json(name = "exerciseName") val exerciseName: String = "",
    @Json(name = "sets") val sets: Int? = null,
    @Json(name = "repsPerSet") val repsPerSet: Int? = null,
    @Json(name = "weightKg") val weightKg: Float? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int = 0,
    @Json(name = "caloriesBurned") val caloriesBurned: Float = 0f
) {
    val reps: Int? get() = repsPerSet
    val durationMin: Int get() = durationMinutes
    val calories: Float get() = caloriesBurned
}

@JsonClass(generateAdapter = true)
data class CreateWorkoutSessionDto(
    @Json(name = "sessionDate") val sessionDate: String,
    @Json(name = "sessionName") val sessionName: String?,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "details") val details: List<WorkoutDetailDto>
)

@JsonClass(generateAdapter = true)
data class WorkoutDetailDto(
    @Json(name = "exerciseId") val exerciseId: String,
    @Json(name = "durationMinutes") val durationMinutes: Int,
    @Json(name = "sets") val sets: Int?,
    @Json(name = "repsPerSet") val repsPerSet: Int?,
    @Json(name = "weightKg") val weightKg: Float?,
    @Json(name = "orderIndex") val orderIndex: Int?
)

@JsonClass(generateAdapter = true)
data class AddExerciseRequest(
    @Json(name = "exerciseId") val exerciseId: String,
    @Json(name = "sets") val sets: Int?,
    @Json(name = "repsPerSet") val repsPerSet: Int?,
    @Json(name = "weightKg") val weightKg: Float?,
    @Json(name = "durationMinutes") val durationMinutes: Int
)

@JsonClass(generateAdapter = true)
data class ActivityLogDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "logDate") val logDate: String = "",
    @Json(name = "steps") val steps: Int = 0,
    @Json(name = "caloriesBurned") val caloriesBurned: Float = 0f,
    @Json(name = "activeMinutes") val activeMinutes: Int = 0,
    @Json(name = "waterMl") val waterMl: Int = 0
) {
    val date: String get() = logDate
    val activeMins: Int get() = activeMinutes
}
