package com.vitalai.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Database(
    entities = [CacheMetaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase()
