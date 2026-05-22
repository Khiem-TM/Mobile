package com.vitalai.data.local.dao

import androidx.room.*
import com.vitalai.data.local.entity.PendingSyncActionEntity

@Dao
interface PendingSyncActionDao {
    @Query("SELECT * FROM pending_sync_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSyncActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: PendingSyncActionEntity): Long

    @Delete
    suspend fun delete(action: PendingSyncActionEntity)

    @Update
    suspend fun update(action: PendingSyncActionEntity)

    @Query("SELECT COUNT(*) FROM pending_sync_actions")
    suspend fun count(): Int
}
