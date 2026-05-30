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
data class ChatSourceDto(
    @Json(name = "title") val title: String,
    @Json(name = "document_id") val documentId: String,
    @Json(name = "chunk_index") val chunkIndex: Int? = null
)

@JsonClass(generateAdapter = true)
data class ChatStreamMetaDto(
    @Json(name = "intent") val intent: String? = null,
    @Json(name = "sources") val sources: List<ChatSourceDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChatStreamDeltaDto(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatStreamDoneDto(
    @Json(name = "message") val message: ChatMessageDto,
    @Json(name = "intent") val intent: String? = null,
    @Json(name = "sources") val sources: List<ChatSourceDto> = emptyList(),
    @Json(name = "disclaimer") val disclaimer: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatStreamErrorDto(
    @Json(name = "message") val message: String? = null
)

sealed class ChatStreamEvent {
    data class Meta(val intent: String, val sources: List<ChatSourceDto>) : ChatStreamEvent()
    data class Delta(val text: String) : ChatStreamEvent()
    data class Done(
        val message: ChatMessageDto,
        val intent: String,
        val sources: List<ChatSourceDto>,
        val disclaimer: String?
    ) : ChatStreamEvent()
    data class Error(val message: String) : ChatStreamEvent()
}

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class CreateChatSessionRequest(
    @Json(name = "title") val title: String?
)
