package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthorUserDto(
    @Json(name = "id") val id: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "email") val email: String?,
    @Json(name = "avatar_url") val avatarUrl: String? = null
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
    @Json(name = "commentCount") val commentCount: Int = 0,
    @Json(name = "rejectionReason") val rejectionReason: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "authorUser") val authorUser: AuthorUserDto? = null,
    @Json(name = "blocks") val blocks: List<BlogBlockDto>? = null
) {
    val displayAuthor: String get() = authorUser?.displayName ?: author ?: "Tracker"
    val firstTag: String? get() = tags?.firstOrNull()
}

@JsonClass(generateAdapter = true)
data class BlogPageDto(
    @Json(name = "items") val items: List<BlogDto>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "limit") val limit: Int
)

@JsonClass(generateAdapter = true)
data class CreateBlogRequest(
    @Json(name = "title") val title: String,
    @Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "blocks") val blocks: List<CreateBlogBlockRequest>? = null
)

@JsonClass(generateAdapter = true)
data class CreateBlogBlockRequest(
    @Json(name = "order") val order: Int,
    @Json(name = "type") val type: String,
    @Json(name = "text_content") val textContent: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null
)
