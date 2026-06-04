package com.vitalai.data.local.room

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import com.vitalai.data.local.room.dao.BodyMetricDao
import com.vitalai.data.local.room.dao.HealthProfileDao
import com.vitalai.data.local.room.dao.MealLogDao
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.dao.UserProfileDao
import com.vitalai.data.local.room.entity.BodyMetricEntity
import com.vitalai.data.local.room.entity.HealthProfileEntity
import com.vitalai.data.local.room.entity.MealLogEntity
import com.vitalai.data.local.room.entity.MealLogItemEntity
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.local.room.entity.UserProfileEntity

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
        BodyMetricEntity::class,
        UserProfileEntity::class,
        HealthProfileEntity::class,
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingSyncActionDao(): PendingSyncActionDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun bodyMetricDao(): BodyMetricDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun healthProfileDao(): HealthProfileDao
}
