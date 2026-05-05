package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FoodDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "brand") val brand: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "image_urls") val imageUrls: List<String>?,
    @Json(name = "serving_size_g") val servingSizeG: Float,
    @Json(name = "serving_unit") val servingUnit: String,
    @Json(name = "calories_per_100g") val caloriesPer100g: Float,
    @Json(name = "carbs_per_100g") val carbsPer100g: Float,
    @Json(name = "protein_per_100g") val proteinPer100g: Float,
    @Json(name = "fat_per_100g") val fatPer100g: Float,
    @Json(name = "fiber_per_100g") val fiberPer100g: Float?,
    @Json(name = "sugar_per_100g") val sugarPer100g: Float?,
    @Json(name = "sodium_per_100g") val sodiumPer100g: Float?,
    @Json(name = "is_verified") val isVerified: Boolean,
    @Json(name = "is_custom") val isCustom: Boolean
) {
    val imageUrl: String? get() = imageUrls?.firstOrNull()
}

@JsonClass(generateAdapter = true)
data class FoodPageDto(
    @Json(name = "items") val items: List<FoodDto>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "limit") val limit: Int
)

@JsonClass(generateAdapter = true)
data class CreateFoodRequest(
    @Json(name = "name") val name: String,
    @Json(name = "brand") val brand: String?,
    @Json(name = "serving_size_g") val servingSizeG: Float,
    @Json(name = "calories_per_100g") val caloriesPer100g: Float,
    @Json(name = "carbs_per_100g") val carbsPer100g: Float,
    @Json(name = "protein_per_100g") val proteinPer100g: Float,
    @Json(name = "fat_per_100g") val fatPer100g: Float,
    @Json(name = "fiber_per_100g") val fiberPer100g: Float?,
    @Json(name = "sugar_per_100g") val sugarPer100g: Float?
)
