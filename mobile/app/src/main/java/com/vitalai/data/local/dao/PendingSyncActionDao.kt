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

    /** Gộp hành động last-write-wins: xoá các hành động cùng loại trước khi chèn cái mới. */
    @Query("DELETE FROM pending_sync_actions WHERE actionType = :actionType")
    suspend fun deleteByActionType(actionType: String)

    /** Lấy hành động ghi mới nhất còn chờ đồng bộ theo loại (để read pending-aware). */
    @Query("SELECT * FROM pending_sync_actions WHERE actionType = :actionType ORDER BY createdAt DESC LIMIT 1")
    suspend fun getByActionType(actionType: String): PendingSyncActionEntity?
}
