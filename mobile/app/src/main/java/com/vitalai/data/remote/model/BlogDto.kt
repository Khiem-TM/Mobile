package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthorUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "email") val email: String?
)

@JsonClass(generateAdapter = true)
data class BlogBlockDto(
    @Json(name = "id") val id: String,
    @Json(name = "order") val order: Int,
    @Json(name = "type") val type: String,
    @Json(name = "textContent") val textContent: String?,
    @Json(name = "imageUrl") val imageUrl: String?
)

@JsonClass(generateAdapter = true)
data class BlogDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "author") val author: String?,
    @Json(name = "content") val content: String?,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String?,
    @Json(name = "tags") val tags: List<String>?,
    @Json(name = "status") val status: String?,
    @Json(name = "likesCount") val likesCount: Int = 0,
    @Json(name = "viewCount") val viewCount: Int = 0,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "authorUser") val authorUser: AuthorUserDto? = null,
    @Json(name = "blocks") val blocks: List<BlogBlockDto>? = null
) {
    val displayAuthor: String get() = authorUser?.displayName ?: author ?: "VitalAI"
    val firstTag: String? get() = tags?.firstOrNull()
}

@JsonClass(generateAdapter = true)
data class BlogPageDto(
    @Json(name = "items") val items: List<BlogDto>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "limit") val limit: Int
)
