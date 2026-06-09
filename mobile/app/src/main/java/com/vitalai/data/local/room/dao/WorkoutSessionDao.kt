package com.vitalai.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vitalai.data.local.room.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_session WHERE sessionDate BETWEEN :from AND :to ORDER BY sessionDate DESC, cachedAt DESC")
    fun observeBetween(from: String, to: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session ORDER BY sessionDate DESC, cachedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session WHERE sessionDate = :date ORDER BY cachedAt DESC")
    fun observeByDate(date: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_session WHERE sessionDate = :date ORDER BY cachedAt DESC")
    suspend fun getByDate(date: String): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<WorkoutSessionEntity>)

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM workout_session WHERE sessionDate = :date AND id <> :keepId")
    suspend fun deleteByDateExcept(date: String, keepId: String)

    @Query("SELECT DISTINCT sessionDate FROM workout_session WHERE isPendingSync = 1")
    suspend fun getPendingDates(): List<String>

    @Query("DELETE FROM workout_session WHERE sessionDate = :date AND isPendingSync = 1")
    suspend fun deletePendingByDate(date: String)

    @Transaction
    suspend fun replaceEntity(tempId: String, entity: WorkoutSessionEntity) {
        deleteById(tempId)
        upsert(entity)
    }

    @Query("DELETE FROM workout_session")
    suspend fun clear()
}
