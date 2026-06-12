package com.vitalai.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vitalai.data.local.room.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY favoritesCount DESC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE isFavorite = 1 ORDER BY name")
    fun observeFavorites(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ExerciseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExerciseEntity)

    @Query("UPDATE exercise SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE exercise SET isFavorite = 0")
    suspend fun clearAllFavoriteFlags()

    @Query("SELECT MIN(cachedAt) FROM exercise")
    suspend fun getOldestCachedAt(): Long?

    @Query("SELECT * FROM exercise WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("DELETE FROM exercise")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(entities: List<ExerciseEntity>) {
        clear()
        upsertAll(entities)
    }
}
