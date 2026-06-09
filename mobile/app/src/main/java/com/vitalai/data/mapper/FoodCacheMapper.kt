package com.vitalai.data.mapper

import com.vitalai.data.local.room.entity.FoodCacheEntity
import com.vitalai.data.remote.model.FoodDto

fun FoodCacheEntity.toDto() = FoodDto(
    id = id,
    name = name,
    brand = brand,
    category = category,
    imageUrlsSnake = imageUrlsSnakePiped?.split("|")?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() },
    imageUrlsCamel = imageUrlsCamelPiped?.split("|")?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() },
    imageUrlRaw = imageUrlRaw,
    imageUrlCamelRaw = imageUrlCamelRaw,
    servingSizeG = servingSizeG,
    servingUnit = servingUnit,
    caloriesPer100g = caloriesPer100g,
    carbsPer100g = carbsPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    isVerified = isVerified,
    isCustom = isCustom
)

fun FoodDto.toCacheEntity(isFavorite: Boolean = false) = FoodCacheEntity(
    id = id,
    name = name,
    brand = brand,
    category = category,
    imageUrlsSnakePiped = imageUrlsSnake?.filter { it.isNotEmpty() }?.joinToString("|"),
    imageUrlsCamelPiped = imageUrlsCamel?.filter { it.isNotEmpty() }?.joinToString("|"),
    imageUrlRaw = imageUrlRaw,
    imageUrlCamelRaw = imageUrlCamelRaw,
    servingSizeG = servingSizeG,
    servingUnit = servingUnit,
    caloriesPer100g = caloriesPer100g,
    carbsPer100g = carbsPer100g,
    proteinPer100g = proteinPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    isVerified = isVerified,
    isCustom = isCustom,
    isFavorite = isFavorite
)
