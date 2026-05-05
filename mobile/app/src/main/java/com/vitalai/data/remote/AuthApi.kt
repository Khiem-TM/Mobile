package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.AuthResponseDto
import com.vitalai.data.remote.model.LoginRequest
import com.vitalai.data.remote.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/google-mobile")
    suspend fun googleMobileLogin(@Body body: Map<String, String>): Response<ApiResponse<AuthResponseDto>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthResponseDto>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponseDto>>

    @POST("auth/refresh")
    suspend fun refresh(@Body refreshToken: Map<String, String>): Response<ApiResponse<AuthResponseDto>>
}
