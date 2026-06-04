package com.vitalai.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vitalai.data.local.room.entity.HealthProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthProfileDao {
    @Query("SELECT * FROM health_profile WHERE id = 'me' LIMIT 1")
    fun observe(): Flow<HealthProfileEntity?>

    @Query("SELECT * FROM health_profile WHERE id = 'me' LIMIT 1")
    suspend fun get(): HealthProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HealthProfileEntity)

    @Query("DELETE FROM health_profile")
    suspend fun clear()
}
