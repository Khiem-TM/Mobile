package com.vitalai.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val primaryMuscleGroup: String,
    val muscleGroupRaw: String?,
    val category: String?,
    val difficultyLevel: String?,
    val equipment: String?,
    val description: String?,
    val instructions: String?,
    val formTips: String?,
    val intensity: String?,
    val thumbnailUrl: String?,
    val videoUrl: String?,
    val imageAvtUrl: String?,
    val imageUrlsPiped: String?,
    val metValue: Float,
    val caloriesPerMin: Float,
    val estimatedCaloriesPerMinute: Float?,
    val favoritesCount: Int,
    val isFavorite: Boolean,
    val secondaryMuscleGroupsPiped: String?,
    val defaultSets: Int?,
    val defaultReps: Int?,
    val defaultWeightKg: Float?,
    val targetMuscleGroup: String?,
    val restTimeSeconds: Int?,
    val defaultDurationMinutes: Int?,
    val defaultIntensityLevel: String?,
    val movementType: String?,
    val exerciseType: String,
    val lottieAsset: String? = null,
    val cachedAt: Long = System.currentTimeMillis()
)
