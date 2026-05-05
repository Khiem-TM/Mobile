package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlogDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "summary") val summary: String?,
    @Json(name = "content") val content: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "tag") val tag: String?,
    @Json(name = "author_name") val authorName: String?,
    @Json(name = "read_time_min") val readTimeMin: Int?,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class BlogPageDto(
    @Json(name = "data") val data: List<BlogDto>,
    @Json(name = "total") val total: Int,
    @Json(name = "page") val page: Int,
    @Json(name = "limit") val limit: Int
)
