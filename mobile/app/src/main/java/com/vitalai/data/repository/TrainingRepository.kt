package com.vitalai.data.repository

import android.content.Context
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.UpdateWorkoutSessionRequest
import com.vitalai.data.remote.model.WorkoutSessionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingRepository @Inject constructor(
    private val trainingApi: TrainingApi,
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("training_prefs", Context.MODE_PRIVATE)
    suspend fun getSessions(date: String? = null): Result<List<WorkoutSessionDto>> {
        return try {
            val response = if (date == null) {
                trainingApi.getSessionHistory()
            } else {
                trainingApi.getSessionsByDate(date)
            }
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExercises(muscleGroup: String? = null): Result<List<ExerciseDto>> {
        return try {
            val response = trainingApi.getExercises(muscleGroup)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavorites(): Result<List<ExerciseDto>> {
        return try {
            val response = trainingApi.getFavoriteExercises()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFavorite(id: String): Result<Unit> {
        return try {
            val response = trainingApi.addFavorite(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi thêm yêu thích (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(id: String): Result<Unit> {
        return try {
            val response = trainingApi.removeFavorite(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi bỏ yêu thích (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSession(request: CreateWorkoutSessionDto): Result<WorkoutSessionDto> {
        return try {
            val response = trainingApi.createSession(request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tạo phiên tập (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSession(id: String): Result<Unit> {
        return try {
            val response = trainingApi.deleteSession(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xóa phiên tập (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSession(id: String, request: UpdateWorkoutSessionRequest): Result<WorkoutSessionDto> {
        return try {
            val response = trainingApi.updateSession(id, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi cập nhật phiên tập (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExercise(sessionId: String, request: AddExerciseRequest): Result<SessionExerciseDto> {
        return try {
            val response = trainingApi.addExercise(sessionId, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi thêm bài tập (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeExercise(sessionId: String, detailId: String): Result<Unit> {
        return try {
            val response = trainingApi.removeExercise(sessionId, detailId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xóa bài tập khỏi phiên (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivityLog(date: String): Result<ActivityLogDto> {
        return try {
            val response = trainingApi.getActivityLog(date)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                // Fetch daily workout sessions to sum workout calories and duration
                val sessionsResponse = trainingApi.getSessionsByDate(date)
                val sessions = sessionsResponse.body()?.data ?: emptyList()
                val workoutCalories = sessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
                val workoutMinutes = sessions.sumOf { it.totalDurationMinutes }

                // Fetch local manual calories and minutes
                val manualCalories = sharedPrefs.getFloat("manual_calories_$date", 0f)
                val manualMinutes = sharedPrefs.getInt("manual_minutes_$date", 0)

                Result.success(body.copy(
                    caloriesBurned = workoutCalories + manualCalories,
                    activeMinutes = workoutMinutes + manualMinutes
                ))
            } else {
                Result.failure(Exception("Lỗi tải hoạt động"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSteps(steps: Int): Result<ActivityLogDto> {
        return try {
            val today = LocalDate.now().toString()
            val response = trainingApi.updateSteps(mapOf("logDate" to today, "steps" to steps))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                val sessionsResponse = trainingApi.getSessionsByDate(today)
                val sessions = sessionsResponse.body()?.data ?: emptyList()
                val workoutCalories = sessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
                val workoutMinutes = sessions.sumOf { it.totalDurationMinutes }

                val manualCalories = sharedPrefs.getFloat("manual_calories_$today", 0f)
                val manualMinutes = sharedPrefs.getInt("manual_minutes_$today", 0)

                Result.success(body.copy(
                    caloriesBurned = workoutCalories + manualCalories,
                    activeMinutes = workoutMinutes + manualMinutes
                ))
            } else {
                Result.failure(Exception("Lỗi cập nhật bước chân"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWater(waterMl: Int): Result<ActivityLogDto> {
        return try {
            val today = LocalDate.now().toString()
            val response = trainingApi.updateWater(mapOf("logDate" to today, "waterMl" to waterMl))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                val sessionsResponse = trainingApi.getSessionsByDate(today)
                val sessions = sessionsResponse.body()?.data ?: emptyList()
                val workoutCalories = sessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
                val workoutMinutes = sessions.sumOf { it.totalDurationMinutes }

                val manualCalories = sharedPrefs.getFloat("manual_calories_$today", 0f)
                val manualMinutes = sharedPrefs.getInt("manual_minutes_$today", 0)

                Result.success(body.copy(
                    caloriesBurned = workoutCalories + manualCalories,
                    activeMinutes = workoutMinutes + manualMinutes
                ))
            } else {
                Result.failure(Exception("Lỗi cập nhật nước uống"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCaloriesBurned(
        caloriesBurned: Float,
        activeMinutes: Int,
        logDate: String = LocalDate.now().toString(),
        exerciseNotes: String? = null
    ): Result<ActivityLogDto> {
        return try {
            // Save manual inputs to local SharedPreferences
            sharedPrefs.edit()
                .putFloat("manual_calories_$logDate", caloriesBurned)
                .putInt("manual_minutes_$logDate", activeMinutes)
                .apply()

            // Fetch daily activity log from backend to get steps and water
            val logResponse = trainingApi.getActivityLog(logDate)
            val log = logResponse.body()?.data ?: ActivityLogDto(logDate = logDate)

            // Fetch training sessions to sum workout calories and duration
            val sessionsResponse = trainingApi.getSessionsByDate(logDate)
            val sessions = sessionsResponse.body()?.data ?: emptyList()
            val workoutCalories = sessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
            val workoutMinutes = sessions.sumOf { it.totalDurationMinutes }

            Result.success(log.copy(
                caloriesBurned = workoutCalories + caloriesBurned,
                activeMinutes = workoutMinutes + activeMinutes
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
