package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FoodBriefDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "image_urls") val imageUrls: List<String>?
) {
    val imageUrl: String? get() = imageUrls?.firstOrNull()
}

@JsonClass(generateAdapter = true)
data class MealLogItemDto(
    @Json(name = "id") val id: String,
    @Json(name = "food_id") val foodId: String,
    @Json(name = "food") val food: FoodBriefDto?,
    @Json(name = "quantity") val quantity: Float,
    @Json(name = "serving_unit") val servingUnit: String,
    @Json(name = "calories_snapshot") val calories: Float,
    @Json(name = "carbs_snapshot") val carbsG: Float,
    @Json(name = "protein_snapshot") val proteinG: Float,
    @Json(name = "fat_snapshot") val fatG: Float
) {
    val foodName: String get() = food?.name ?: ""
    val imageUrl: String? get() = food?.imageUrl
}

@JsonClass(generateAdapter = true)
data class MealLogDto(
    @Json(name = "id") val id: String,
    @Json(name = "meal_type") val mealType: String,
    @Json(name = "log_date") val date: String,
    @Json(name = "items") val items: List<MealLogItemDto>
) {
    val totalCalories: Float get() = items.sumOf { it.calories.toDouble() }.toFloat()
}

@JsonClass(generateAdapter = true)
data class MealLogSummaryDto(
    @Json(name = "total_calories") val totalCalories: Float,
    @Json(name = "total_carbs") val totalCarbs: Float,
    @Json(name = "total_protein") val totalProtein: Float,
    @Json(name = "total_fat") val totalFat: Float
)

@JsonClass(generateAdapter = true)
data class CreateMealLogRequest(
    @Json(name = "meal_type") val mealType: String,
    @Json(name = "log_date") val logDate: String
)

@JsonClass(generateAdapter = true)
data class AddMealItemRequest(
    @Json(name = "food_id") val foodId: String,
    @Json(name = "quantity") val quantity: Float,
    @Json(name = "serving_unit") val servingUnit: String
)
