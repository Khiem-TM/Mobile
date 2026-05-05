package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.NotificationDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationsApi {
    @GET("notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<NotificationDto>>>

    @PATCH("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<ApiResponse<NotificationDto>>

    @PATCH("notifications/read-all")
    suspend fun markAllRead(): Response<ApiResponse<Unit>>
}
