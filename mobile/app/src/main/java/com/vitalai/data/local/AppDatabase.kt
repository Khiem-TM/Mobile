package com.vitalai.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import com.vitalai.data.local.dao.MealLogDao
import com.vitalai.data.local.dao.PendingSyncActionDao
import com.vitalai.data.local.entity.MealLogEntity
import com.vitalai.data.local.entity.MealLogItemEntity
import com.vitalai.data.local.entity.PendingSyncActionEntity

@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Database(
    entities = [
        CacheMetaEntity::class,
        PendingSyncActionEntity::class,
        MealLogEntity::class,
        MealLogItemEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingSyncActionDao(): PendingSyncActionDao
    abstract fun mealLogDao(): MealLogDao
}
