package com.vitalai.data.remote

import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TrainingApi {
    @GET("training/sessions")
    suspend fun getSessionHistory(): Response<ApiResponse<List<WorkoutSessionDto>>>

    @GET("training/sessions/date/{date}")
    suspend fun getSessionsByDate(@Path("date") date: String): Response<ApiResponse<List<WorkoutSessionDto>>>

    @GET("training/exercises")
    suspend fun getExercises(@Query("muscleGroup") muscleGroup: String? = null): Response<ApiResponse<List<ExerciseDto>>>

    @GET("training/exercises/favorites")
    suspend fun getFavoriteExercises(): Response<ApiResponse<List<ExerciseDto>>>

    @POST("training/exercises/{id}/favorite")
    suspend fun addFavorite(@Path("id") id: String): Response<ApiResponse<Map<String, String>>>

    @DELETE("training/exercises/{id}/favorite")
    suspend fun removeFavorite(@Path("id") id: String): Response<ApiResponse<Map<String, String>>>

    @POST("training/sessions")
    suspend fun createSession(@Body request: CreateWorkoutSessionDto): Response<ApiResponse<WorkoutSessionDto>>

    @DELETE("training/sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String): Response<Unit>

    @POST("training/sessions/{id}/exercises")
    suspend fun addExercise(
        @Path("id") sessionId: String,
        @Body request: AddExerciseRequest
    ): Response<ApiResponse<SessionExerciseDto>>

    @GET("activity-logs")
    suspend fun getActivityLog(@Query("date") date: String): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/steps")
    suspend fun updateSteps(@Body body: Map<String, Any>): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/water")
    suspend fun updateWater(@Body body: Map<String, Any>): Response<ApiResponse<ActivityLogDto>>
}
