package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatSessionDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "last_message") val lastMessage: String?
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class CreateChatSessionRequest(
    @Json(name = "title") val title: String?
)
