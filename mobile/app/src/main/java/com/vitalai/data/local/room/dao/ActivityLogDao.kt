package com.vitalai.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vitalai.data.local.room.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_log WHERE logDate = :date LIMIT 1")
    fun observeByDate(date: String): Flow<ActivityLogEntity?>

    @Query("SELECT * FROM activity_log WHERE logDate BETWEEN :from AND :to ORDER BY logDate")
    fun observeBetween(from: String, to: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_log WHERE logDate = :date LIMIT 1")
    suspend fun getByDate(date: String): ActivityLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActivityLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ActivityLogEntity>)

    @Query("DELETE FROM activity_log")
    suspend fun clear()
}
