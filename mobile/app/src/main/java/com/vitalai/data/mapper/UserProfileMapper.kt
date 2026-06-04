package com.vitalai.data.mapper

import com.vitalai.data.local.room.entity.HealthProfileEntity
import com.vitalai.data.local.room.entity.UserProfileEntity
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.UserDto

fun UserProfileEntity.toDto() = UserDto(
    id = userId,
    email = email,
    displayName = displayName,
    avatarUrlRaw = avatarUrlRaw,
    role = role,
    isVerified = true
)

fun UserDto.toEntity() = UserProfileEntity(
    id = "me",
    userId = id,
    email = email,
    displayName = displayName,
    avatarUrlRaw = avatarUrlRaw,
    role = role
)

fun HealthProfileEntity.toDto() = HealthProfileDto(
    birthDate = birthDate,
    gender = gender,
    heightCm = heightCm,
    initialWeightKg = initialWeightKg,
    activityLevel = activityLevel,
    dietType = dietType,
    foodAllergies = foodAllergies?.split("|")?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() },
    caloriesGoal = caloriesGoal,
    goalType = goalType,
    targetWeightKg = targetWeightKg,
    dailyCaloriesGoal = dailyCaloriesGoal,
    proteinGoalG = proteinGoalG,
    fatGoalG = fatGoalG,
    carbsGoalG = carbsGoalG,
    weeklyRateKg = weeklyRateKg,
    goalStartDate = goalStartDate,
    goalDeadline = goalDeadline,
    goalStatus = goalStatus,
    waterGoalMl = waterGoalMl,
    stepGoal = stepGoal
)

fun HealthProfileDto.toEntity() = HealthProfileEntity(
    id = "me",
    birthDate = birthDate,
    gender = gender,
    heightCm = heightCm,
    initialWeightKg = initialWeightKg,
    activityLevel = activityLevel,
    dietType = dietType,
    foodAllergies = foodAllergies?.filter { it.isNotEmpty() }?.joinToString("|"),
    caloriesGoal = caloriesGoal,
    goalType = goalType,
    targetWeightKg = targetWeightKg,
    dailyCaloriesGoal = dailyCaloriesGoal,
    proteinGoalG = proteinGoalG,
    fatGoalG = fatGoalG,
    carbsGoalG = carbsGoalG,
    weeklyRateKg = weeklyRateKg,
    goalStartDate = goalStartDate,
    goalDeadline = goalDeadline,
    goalStatus = goalStatus,
    waterGoalMl = waterGoalMl,
    stepGoal = stepGoal
)
