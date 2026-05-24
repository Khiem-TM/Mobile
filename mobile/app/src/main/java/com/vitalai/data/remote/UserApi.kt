package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.UpdateProfileRequest
import com.vitalai.data.remote.model.UserDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT

interface UserApi {
    @GET("users/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    @PATCH("users/{id}")
    suspend fun updateProfile(
        @Path("id") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserDto>>

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @GET("users/me/health-profile")
    suspend fun getHealthProfile(): Response<ApiResponse<HealthProfileDto>>

    @PUT("users/me/health-profile")
    suspend fun updateHealthProfile(@Body profile: HealthProfileDto): Response<ApiResponse<HealthProfileDto>>
}
