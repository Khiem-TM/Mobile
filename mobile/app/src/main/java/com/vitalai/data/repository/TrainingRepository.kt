package com.vitalai.data.repository

import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.CreateSessionRequest
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingRepository @Inject constructor(
    private val trainingApi: TrainingApi
) {
    suspend fun getSessions(date: String? = null): Result<List<WorkoutSessionDto>> {
        return try {
            val response = trainingApi.getSessions(date)
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

    suspend fun createSession(request: CreateSessionRequest): Result<WorkoutSessionDto> {
        return try {
            val response = trainingApi.createSession(request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tạo phiên tập (${response.code()})"))
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
            val response = trainingApi.updateSteps(mapOf("steps" to steps))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi cập nhật bước chân"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWater(waterMl: Int): Result<ActivityLogDto> {
        return try {
            val response = trainingApi.updateWater(mapOf("water_ml" to waterMl))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi cập nhật nước uống"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
