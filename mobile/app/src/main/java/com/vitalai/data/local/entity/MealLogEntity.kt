package com.vitalai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_logs")
data class MealLogEntity(
    @PrimaryKey val id: String,
    val mealType: String,
    val date: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "meal_log_items",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = MealLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealLogId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class MealLogItemEntity(
    @PrimaryKey val id: String,
    val mealLogId: String,
    val foodId: String,
    val foodName: String,
    val imageUrl: String?,
    val quantity: Float,
    val servingUnit: String,
    val calories: Float,
    val carbsG: Float,
    val proteinG: Float,
    val fatG: Float
)
