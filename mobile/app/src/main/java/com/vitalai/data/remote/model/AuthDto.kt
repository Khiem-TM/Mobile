package com.vitalai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vitalai.core.network.ImageUrlResolver

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "user") val user: UserDto
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val email: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "avatar_url") val avatarUrlRaw: String?,
    val role: String,
    @Json(name = "is_verified") val isVerified: Boolean
) {
    /** Normalized so relative backend paths become loadable URLs. */
    val avatarUrl: String? get() = ImageUrlResolver.resolve(avatarUrlRaw)
}

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Int
)
