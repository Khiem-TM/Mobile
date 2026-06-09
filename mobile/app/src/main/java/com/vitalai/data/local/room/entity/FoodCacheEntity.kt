package com.vitalai.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_cache")
data class FoodCacheEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val category: String?,
    val imageUrlsSnakePiped: String?,
    val imageUrlsCamelPiped: String?,
    val imageUrlRaw: String?,
    val imageUrlCamelRaw: String?,
    val servingSizeG: Float,
    val servingUnit: String,
    val caloriesPer100g: Float,
    val carbsPer100g: Float,
    val proteinPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float?,
    val isVerified: Boolean,
    val isCustom: Boolean,
    val isFavorite: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)
