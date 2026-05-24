package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.UpdateProfileRequest
import com.vitalai.data.remote.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.PUT

interface UserApi {
    @GET("users/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    @PATCH("users/{id}")
    suspend fun updateProfile(
        @Path("id") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserDto>>

    @GET("users/me/health-profile")
    suspend fun getHealthProfile(): Response<ApiResponse<HealthProfileDto>>

    @PUT("users/me/health-profile")
    suspend fun updateHealthProfile(@Body profile: HealthProfileDto): Response<ApiResponse<HealthProfileDto>>
}
