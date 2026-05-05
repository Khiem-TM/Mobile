package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    @Json(name = "statusCode") val statusCode: Int,
    val data: T?,
    val message: String? = null
)
