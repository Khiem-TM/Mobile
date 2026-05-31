package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    // Backend entity column is `body` (notifications.body), not `message`.
    @Json(name = "body") val message: String,
    @Json(name = "type") val type: String,
    // Backend (TypeORM) serialize camelCase: isRead/createdAt (KHÔNG phải snake_case).
    @Json(name = "isRead") val isRead: Boolean,
    @Json(name = "createdAt") val createdAt: String
)
