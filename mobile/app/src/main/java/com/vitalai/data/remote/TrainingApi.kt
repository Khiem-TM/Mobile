package com.vitalai.data.remote

import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.CreateSessionRequest
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TrainingApi {
    @GET("training/sessions")
    suspend fun getSessions(@Query("date") date: String? = null): Response<ApiResponse<List<WorkoutSessionDto>>>

    @GET("training/exercises")
    suspend fun getExercises(@Query("muscleGroup") muscleGroup: String? = null): Response<ApiResponse<List<ExerciseDto>>>

    @POST("training/sessions")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<ApiResponse<WorkoutSessionDto>>

    @POST("training/sessions/{id}/exercises")
    suspend fun addExercise(
        @Path("id") sessionId: String,
        @Body request: AddExerciseRequest
    ): Response<ApiResponse<SessionExerciseDto>>

    @GET("activity-logs")
    suspend fun getActivityLog(@Query("date") date: String): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/steps")
    suspend fun updateSteps(@Body body: Map<String, Int>): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/water")
    suspend fun updateWater(@Body body: Map<String, Int>): Response<ApiResponse<ActivityLogDto>>
}
