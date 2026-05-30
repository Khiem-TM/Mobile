package com.vitalai.data.repository

import com.vitalai.data.remote.DashboardApi
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.DashboardMonthlyDto
import com.vitalai.data.remote.model.DashboardWeeklyDto
import com.vitalai.data.remote.model.StreakDto
import com.vitalai.data.remote.model.DailyDashboardResponse
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val trainingApi: TrainingApi,
    private val userRepository: UserRepository
) {
    suspend fun getDashboard(date: String? = null): Result<DashboardDto> {
        return try {
            val targetDate = date ?: LocalDate.now().toString()
            val response = dashboardApi.getDashboard(targetDate)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                // Fetch user health profile to populate calorie and nutrient goals
                val profileResult = userRepository.getHealthProfile()
                val profile = profileResult.getOrNull()

                // Fetch training sessions to sum daily workout calories burned
                val sessionsResponse = trainingApi.getSessionsByDate(targetDate)
                val sessions = sessionsResponse.body()?.data ?: emptyList()
                val caloriesBurned = sessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()

                // Construct flat DashboardDto from nested response and goals
                val flatDashboard = DashboardDto(
                    calorieGoal = profile?.caloriesGoal?.toInt() ?: 2000,
                    caloriesConsumed = body.nutrition.totalCalories,
                    caloriesBurned = caloriesBurned,
                    carbsG = body.nutrition.totalCarbs,
                    carbsGoal = profile?.carbsGoalG ?: 250f,
                    proteinG = body.nutrition.totalProtein,
                    proteinGoal = profile?.proteinGoalG ?: 150f,
                    fatG = body.nutrition.totalFat,
                    fatGoal = profile?.fatGoalG ?: 65f,
                    waterMl = body.activity.waterMl,
                    waterGoalMl = profile?.waterGoalMl ?: 2000,
                    steps = body.activity.steps,
                    stepGoal = profile?.stepGoal ?: 10000
                )
                Result.success(flatDashboard)
            } else {
                Result.failure(Exception("Lỗi tải dashboard (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStreaks(): Result<StreakDto> {
        return try {
            val response = dashboardApi.getStreaks()
            val list = response.body()?.data
            if (response.isSuccessful && list != null) {
                // Backend sends a list of per-type streaks (login/calorie_goal/workout);
                // flatten to the object the UI consumes.
                fun current(type: String) = list.firstOrNull { it.streakType == type }?.currentStreak ?: 0
                Result.success(
                    StreakDto(
                        loginStreak = current("login"),
                        mealLogStreak = current("calorie_goal"),
                        workoutStreak = current("workout")
                    )
                )
            } else Result.failure(Exception("Lỗi tải streak (${response.code()})"))
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

    suspend fun addWater(ml: Int, date: String? = null): Result<DashboardDto> {
        return try {
            val targetDate = date ?: LocalDate.now().toString()
            val response = trainingApi.updateWater(mapOf("logDate" to targetDate, "waterMl" to ml))
            if (response.isSuccessful) getDashboard(targetDate)
            else Result.failure(Exception("Lỗi cập nhật nước (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSteps(steps: Int, date: String? = null): Result<DashboardDto> {
        return try {
            val targetDate = date ?: LocalDate.now().toString()
            val response = trainingApi.updateSteps(mapOf("logDate" to targetDate, "steps" to steps))
            if (response.isSuccessful) getDashboard(targetDate)
            else Result.failure(Exception("Lỗi cập nhật bước chân (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
