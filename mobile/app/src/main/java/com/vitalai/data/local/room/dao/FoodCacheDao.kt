package com.vitalai.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vitalai.data.local.room.entity.FoodCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodCacheDao {
    @Query("SELECT * FROM food_cache WHERE isFavorite = 1 ORDER BY name")
    fun observeFavorites(): Flow<List<FoodCacheEntity>>

    @Query("SELECT * FROM food_cache WHERE isCustom = 1 ORDER BY name")
    fun observeCustomFoods(): Flow<List<FoodCacheEntity>>

    @Query("SELECT id FROM food_cache WHERE isFavorite = 1")
    suspend fun getFavoriteIds(): List<String>

    @Query("SELECT * FROM food_cache WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FoodCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FoodCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FoodCacheEntity)

    @Query("UPDATE food_cache SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE food_cache SET isFavorite = 0")
    suspend fun clearFavoriteFlags()

    @Query("DELETE FROM food_cache WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM food_cache")
    suspend fun clear()
}
