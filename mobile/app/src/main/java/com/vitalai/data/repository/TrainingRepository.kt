package com.vitalai.data.repository

import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.UpdateCaloriesBurnedRequest
import com.vitalai.data.remote.model.UpdateWorkoutSessionRequest
import com.vitalai.data.remote.model.WorkoutSessionDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingRepository @Inject constructor(
    private val trainingApi: TrainingApi
) {
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
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải hoạt động"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSteps(steps: Int): Result<ActivityLogDto> {
        return try {
            val today = LocalDate.now().toString()
            val response = trainingApi.updateSteps(mapOf("logDate" to today, "steps" to steps))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi cập nhật bước chân"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWater(waterMl: Int): Result<ActivityLogDto> {
        return try {
            val today = LocalDate.now().toString()
            val response = trainingApi.updateWater(mapOf("logDate" to today, "waterMl" to waterMl))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi cập nhật nước uống"))
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
            val response = trainingApi.updateCaloriesBurned(
                UpdateCaloriesBurnedRequest(
                    logDate = logDate,
                    caloriesBurned = caloriesBurned,
                    activeMinutes = activeMinutes,
                    exerciseNotes = exerciseNotes
                )
            )
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi cập nhật calories đã đốt (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
