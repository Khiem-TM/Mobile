package com.vitalai.data.repository

import com.vitalai.data.remote.DashboardApi
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.DashboardMonthlyDto
import com.vitalai.data.remote.model.DashboardWeeklyDto
import com.vitalai.data.remote.model.StreakDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val trainingApi: TrainingApi
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

    suspend fun getWeeklyDashboard(weekStart: String): Result<DashboardWeeklyDto> {
        return try {
            val response = dashboardApi.getWeeklyDashboard(weekStart)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải thống kê tuần (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMonthlyDashboard(year: Int, month: Int): Result<DashboardMonthlyDto> {
        return try {
            val response = dashboardApi.getMonthlyDashboard(year, month)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải thống kê tháng (${response.code()})"))
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

    suspend fun addWater(ml: Int): Result<DashboardDto> {
        return try {
            val today = LocalDate.now().toString()
            val response = trainingApi.updateWater(mapOf("logDate" to today, "waterMl" to ml))
            if (response.isSuccessful) getDashboard(today)
            else Result.failure(Exception("Lỗi cập nhật nước (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSteps(steps: Int): Result<DashboardDto> {
        return try {
            val today = LocalDate.now().toString()
            val response = trainingApi.updateSteps(mapOf("logDate" to today, "steps" to steps))
            if (response.isSuccessful) getDashboard(today)
            else Result.failure(Exception("Lỗi cập nhật bước chân (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
