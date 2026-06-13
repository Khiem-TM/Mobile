package com.vitalai.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vitalai.data.local.room.entity.BodyMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {

    @Query("SELECT * FROM body_metrics ORDER BY measuredAt DESC")
    fun observeAll(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics ORDER BY measuredAt DESC LIMIT 1")
    fun observeLatest(): Flow<BodyMetricEntity?>

    @Query("SELECT * FROM body_metrics WHERE bodyFatPct IS NOT NULL OR waistCm IS NOT NULL OR hipCm IS NOT NULL OR chestCm IS NOT NULL OR armCm IS NOT NULL OR neckCm IS NOT NULL ORDER BY measuredAt DESC LIMIT 1")
    fun observeLatestWithMeasurements(): Flow<BodyMetricEntity?>

    @Query("SELECT * FROM body_metrics WHERE measuredAt >= :from AND measuredAt < :to ORDER BY measuredAt ASC")
    fun observeByDateRange(from: String, to: String): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics ORDER BY measuredAt DESC LIMIT 1")
    suspend fun getLatest(): BodyMetricEntity?

    @Query("SELECT MIN(measuredAt) FROM body_metrics")
    fun observeEarliestDate(): Flow<String?>

    @Query("SELECT * FROM body_metrics WHERE measuredAt LIKE :datePrefix || '%' LIMIT 1")
    suspend fun getByDatePrefix(datePrefix: String): BodyMetricEntity?

    @Query("SELECT * FROM body_metrics WHERE isPendingSync = 1")
    suspend fun getAllPendingMetrics(): List<BodyMetricEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BodyMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BodyMetricEntity>)

    @Query("DELETE FROM body_metrics WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun replaceEntity(oldId: String, newEntity: BodyMetricEntity) {
        deleteById(oldId)
        upsert(newEntity)
    }

    @Query("DELETE FROM body_metrics WHERE isPendingSync = 0 AND SUBSTR(measuredAt, 1, 10) NOT IN (:serverDates)")
    suspend fun deleteSyncedExcluding(serverDates: List<String>)

    @Transaction
    suspend fun syncFromServer(toUpsert: List<BodyMetricEntity>, serverDates: List<String>) {
        upsertAll(toUpsert)
        deleteSyncedExcluding(serverDates)
    }

    @Query("DELETE FROM body_metrics")
    suspend fun clearAll()
}
