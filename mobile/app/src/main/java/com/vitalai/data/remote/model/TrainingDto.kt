package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExerciseDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "muscle_group") val muscleGroup: String,
    @Json(name = "equipment") val equipment: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "calories_per_min") val caloriesPerMin: Float
)

@JsonClass(generateAdapter = true)
data class WorkoutSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String?,
    @Json(name = "date") val date: String,
    @Json(name = "duration_min") val durationMin: Int,
    @Json(name = "total_calories") val totalCalories: Float,
    @Json(name = "exercises") val exercises: List<SessionExerciseDto>
)

@JsonClass(generateAdapter = true)
data class SessionExerciseDto(
    @Json(name = "id") val id: String,
    @Json(name = "exercise_id") val exerciseId: String,
    @Json(name = "exercise_name") val exerciseName: String,
    @Json(name = "sets") val sets: Int,
    @Json(name = "reps") val reps: Int,
    @Json(name = "weight_kg") val weightKg: Float?,
    @Json(name = "duration_min") val durationMin: Int?,
    @Json(name = "calories") val calories: Float
)

@JsonClass(generateAdapter = true)
data class CreateSessionRequest(
    @Json(name = "name") val name: String?,
    @Json(name = "date") val date: String
)

@JsonClass(generateAdapter = true)
data class AddExerciseRequest(
    @Json(name = "exercise_id") val exerciseId: String,
    @Json(name = "sets") val sets: Int,
    @Json(name = "reps") val reps: Int,
    @Json(name = "weight_kg") val weightKg: Float?,
    @Json(name = "duration_min") val durationMin: Int?
)

@JsonClass(generateAdapter = true)
data class ActivityLogDto(
    @Json(name = "id") val id: String,
    @Json(name = "date") val date: String,
    @Json(name = "steps") val steps: Int,
    @Json(name = "calories_burned") val caloriesBurned: Float,
    @Json(name = "active_min") val activeMins: Int,
    @Json(name = "water_ml") val waterMl: Int
)
