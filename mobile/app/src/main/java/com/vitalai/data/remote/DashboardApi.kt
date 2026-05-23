package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.StreakDto
import com.vitalai.data.remote.model.UnreadCountDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DashboardApi {
    @GET("dashboard")
    suspend fun getDashboard(@Query("date") date: String? = null): Response<ApiResponse<DashboardDto>>

    @GET("streaks")
    suspend fun getStreaks(): Response<ApiResponse<StreakDto>>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<UnreadCountDto>>

    @POST("dashboard/water")
    suspend fun updateWater(@Body body: Map<String, Int>): Response<ApiResponse<DashboardDto>>

    @POST("dashboard/steps")
    suspend fun updateSteps(@Body body: Map<String, Int>): Response<ApiResponse<DashboardDto>>
}
