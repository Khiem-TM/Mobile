package com.vitalai.data.local.dao

import androidx.room.*
import com.vitalai.data.local.entity.MealLogEntity
import com.vitalai.data.local.entity.MealLogItemEntity
import kotlinx.coroutines.flow.Flow

data class MealLogWithItems(
    @Embedded val log: MealLogEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealLogId"
    )
    val items: List<MealLogItemEntity>
)

@Dao
interface MealLogDao {
    @Transaction
    @Query("SELECT * FROM meal_logs WHERE date = :date ORDER BY mealType ASC")
    fun observeMealLogsWithItems(date: String): Flow<List<MealLogWithItems>>

    @Query("SELECT * FROM meal_logs WHERE date = :date")
    suspend fun getMealLogsByDate(date: String): List<MealLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<MealLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MealLogItemEntity>)

    @Query("DELETE FROM meal_logs WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM meal_log_items WHERE mealLogId = :mealLogId")
    suspend fun deleteItemsByMealLog(mealLogId: String)

    @Query("DELETE FROM meal_log_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("SELECT * FROM meal_log_items WHERE id = :itemId")
    suspend fun getItemById(itemId: String): MealLogItemEntity?
}
