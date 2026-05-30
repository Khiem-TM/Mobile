package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vitalai.core.network.ImageUrlResolver

@JsonClass(generateAdapter = true)
data class ExerciseDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "primaryMuscleGroup") val primaryMuscleGroup: String = "",
    @Json(name = "muscleGroup") val muscleGroupRaw: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "difficultyLevel") val difficultyLevel: String? = null,
    @Json(name = "equipment") val equipment: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "instructions") val instructions: String? = null,
    @Json(name = "formTips") val formTips: String? = null,
    @Json(name = "intensity") val intensity: String? = null,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "videoUrl") val videoUrl: String? = null,
    @Json(name = "imageAvtUrl") val imageAvtUrl: String? = null,
    @Json(name = "imageUrl") val imageUrl: List<String>? = null,
    @Json(name = "metValue") val metValue: Float = 0f,
    @Json(name = "caloriesPerMin") val caloriesPerMin: Float = 0f,
    @Json(name = "estimatedCaloriesPerMinute") val estimatedCaloriesPerMinute: Float? = null,
    @Json(name = "favoritesCount") val favoritesCount: Int = 0,
    @Json(name = "isFavorite") val isFavorite: Boolean = false,
    @Json(name = "secondaryMuscleGroups") val secondaryMuscleGroups: List<String>? = null,
    @Json(name = "defaultSets") val defaultSets: Int? = null,
    @Json(name = "defaultReps") val defaultReps: Int? = null,
    @Json(name = "defaultWeightKg") val defaultWeightKg: Float? = null,
    @Json(name = "targetMuscleGroup") val targetMuscleGroup: String? = null,
    @Json(name = "restTimeSeconds") val restTimeSeconds: Int? = null,
    @Json(name = "defaultDurationMinutes") val defaultDurationMinutes: Int? = null,
    @Json(name = "defaultIntensityLevel") val defaultIntensityLevel: String? = null,
    @Json(name = "movementType") val movementType: String? = null,
    @Json(name = "exerciseType") val exerciseType: String = "SPORT"
) {
    val displayImageUrl: String? get() = ImageUrlResolver.resolve(
        thumbnailUrl ?: imageAvtUrl ?: imageUrl?.firstOrNull()
    )
    val muscleGroup: String get() = muscleGroupRaw ?: primaryMuscleGroup
}

@JsonClass(generateAdapter = true)
data class WorkoutSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val sessionName: String? = null,
    @Json(name = "sessionDate") val sessionDate: String,
    @Json(name = "totalDurationMinutes") val totalDurationMinutes: Int = 0,
    @Json(name = "totalCaloriesBurned") val totalCaloriesBurned: Float = 0f,
    @Json(name = "items") val details: List<SessionExerciseDto> = emptyList()
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
    @Json(name = "exercise") val exercise: ExerciseDto? = null,
    @Json(name = "sets") val sets: Int? = null,
    @Json(name = "reps") val repsPerSet: Int? = null,
    @Json(name = "weightKg") val weightKg: Float? = null,
    @Json(name = "durationMinutes") val durationMinutes: Int = 0,
    @Json(name = "caloriesBurned") val caloriesBurned: Float = 0f,
    @Json(name = "exerciseType") val exerciseType: String = "SPORT",
    @Json(name = "intensityLevel") val intensityLevel: String? = null,
    @Json(name = "distanceKm") val distanceKm: Float? = null,
    @Json(name = "avgSpeedKmh") val avgSpeedKmh: Float? = null,
    @Json(name = "restTimeSeconds") val restTimeSeconds: Int? = null
) {
    val exerciseName: String get() = exercise?.name ?: ""
    val reps: Int? get() = repsPerSet
    val durationMin: Int get() = durationMinutes
    val calories: Float get() = caloriesBurned
}

@JsonClass(generateAdapter = true)
data class CreateWorkoutSessionDto(
    @Json(name = "sessionDate") val sessionDate: String,
    @Json(name = "title") val sessionName: String?,
    @Json(name = "note") val notes: String? = null,
    @Json(name = "items") val details: List<WorkoutDetailDto>
)

@JsonClass(generateAdapter = true)
data class WorkoutDetailDto(
    @Json(name = "exerciseId") val exerciseId: String,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "sets") val sets: Int?,
    @Json(name = "reps") val repsPerSet: Int?,
    @Json(name = "weightKg") val weightKg: Float?,
    @Json(name = "orderIndex") val orderIndex: Int?,
    @Json(name = "exerciseType") val exerciseType: String = "SPORT",
    @Json(name = "intensityLevel") val intensityLevel: String? = null,
    @Json(name = "distanceKm") val distanceKm: Float? = null,
    @Json(name = "avgSpeedKmh") val avgSpeedKmh: Float? = null,
    @Json(name = "restTimeSeconds") val restTimeSeconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class AddExerciseRequest(
    @Json(name = "exerciseId") val exerciseId: String,
    @Json(name = "exerciseType") val exerciseType: String = "SPORT",
    @Json(name = "sets") val sets: Int?,
    @Json(name = "reps") val repsPerSet: Int?,
    @Json(name = "weightKg") val weightKg: Float?,
    @Json(name = "durationMinutes") val durationMinutes: Int? = null,
    @Json(name = "orderIndex") val orderIndex: Int = 0,
    @Json(name = "intensityLevel") val intensityLevel: String? = null,
    @Json(name = "distanceKm") val distanceKm: Float? = null,
    @Json(name = "avgSpeedKmh") val avgSpeedKmh: Float? = null,
    @Json(name = "restTimeSeconds") val restTimeSeconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class UpdateWorkoutSessionRequest(
    @Json(name = "sessionDate") val sessionDate: String? = null,
    @Json(name = "title") val sessionName: String? = null,
    @Json(name = "note") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ActivityLogDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "logDate") val logDate: String = "",
    @Json(name = "steps") val steps: Int = 0,
    @Json(name = "caloriesBurned") val caloriesBurned: Float = 0f,
    @Json(name = "activeMinutes") val activeMinutes: Int = 0,
    @Json(name = "waterMl") val waterMl: Int = 0,
    @Json(name = "sleepHours") val sleepHours: Float? = null,
    @Json(name = "mood") val mood: String? = null,
    @Json(name = "note") val note: String? = null
) {
    val date: String get() = logDate
    val activeMins: Int get() = activeMinutes
}
