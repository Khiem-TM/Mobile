package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.DailyDashboardResponse
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.DashboardMonthlyDto
import com.vitalai.data.remote.model.DashboardWeeklyDto
import com.vitalai.data.remote.model.DailyStreakDto
import com.vitalai.data.remote.model.UnreadCountDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {
    @GET("dashboard")
    suspend fun getDashboard(@Query("date") date: String? = null): Response<ApiResponse<DailyDashboardResponse>>

    @GET("dashboard/weekly")
    suspend fun getWeeklyDashboard(@Query("weekStart") weekStart: String): Response<ApiResponse<DashboardWeeklyDto>>

    @GET("dashboard/monthly")
    suspend fun getMonthlyDashboard(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<ApiResponse<DashboardMonthlyDto>>

    // Backend returns an ARRAY of per-type streak rows, not a single object.
    @GET("streaks")
    suspend fun getStreaks(): Response<ApiResponse<List<DailyStreakDto>>>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<UnreadCountDto>>
}
