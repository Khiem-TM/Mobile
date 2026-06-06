package com.vitalai.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrlRaw: String?,
    val role: String,
    val cachedAt: Long = System.currentTimeMillis()
)
