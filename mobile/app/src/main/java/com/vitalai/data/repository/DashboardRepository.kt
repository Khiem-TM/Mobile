package com.vitalai.data.repository

import com.vitalai.data.remote.DashboardApi
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.StreakDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardApi: DashboardApi
) {
    suspend fun getDashboard(date: String? = null): Result<DashboardDto> {
        return try {
            val response = dashboardApi.getDashboard(date)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải dashboard (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStreaks(): Result<StreakDto> {
        return try {
            val response = dashboardApi.getStreaks()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải streak (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadCount(): Result<Int> {
        return try {
            val response = dashboardApi.getUnreadCount()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body.count)
            else Result.success(0)
        } catch (e: Exception) {
            Result.success(0)
        }
    }
}
