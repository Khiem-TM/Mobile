package com.vitalai.data.remote

import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.UpdateWorkoutSessionRequest
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
    @GET("training-sessions")
    suspend fun getSessionHistory(
        @Query("limit") limit: Int? = null,
        @Query("fromDate") fromDate: String? = null,
        @Query("toDate") toDate: String? = null
    ): Response<ApiResponse<List<WorkoutSessionDto>>>

    @GET("training-sessions/date/{date}")
    suspend fun getSessionsByDate(@Path("date") date: String): Response<ApiResponse<List<WorkoutSessionDto>>>

    @GET("exercises")
    suspend fun getExercises(
        @Query("exerciseType") exerciseType: String? = null,
        @Query("name") name: String? = null,
        @Query("muscleGroup") muscleGroup: String? = null,
        @Query("difficultyLevel") difficultyLevel: String? = null
    ): Response<ApiResponse<List<ExerciseDto>>>

    @GET("exercises/{id}")
    suspend fun getExerciseById(@Path("id") id: String): Response<ApiResponse<ExerciseDto>>

    @GET("favorite-exercises")
    suspend fun getFavoriteExercises(): Response<ApiResponse<List<ExerciseDto>>>

    @POST("favorite-exercises/{id}")
    suspend fun addFavorite(@Path("id") id: String): Response<ApiResponse<Map<String, String>>>

    @DELETE("favorite-exercises/{id}")
    suspend fun removeFavorite(@Path("id") id: String): Response<ApiResponse<Map<String, String>>>

    @POST("training-sessions")
    suspend fun createSession(@Body request: CreateWorkoutSessionDto): Response<ApiResponse<WorkoutSessionDto>>

    @DELETE("training-sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String): Response<Unit>

    @PATCH("training-sessions/{id}")
    suspend fun updateSession(
        @Path("id") id: String,
        @Body request: UpdateWorkoutSessionRequest
    ): Response<ApiResponse<WorkoutSessionDto>>

    @POST("training-sessions/{id}/items")
    suspend fun addExercise(
        @Path("id") sessionId: String,
        @Body request: AddExerciseRequest
    ): Response<ApiResponse<SessionExerciseDto>>

    @DELETE("training-sessions/{id}/items/{detailId}")
    suspend fun removeExercise(
        @Path("id") sessionId: String,
        @Path("detailId") detailId: String
    ): Response<Unit>

    @GET("activity-logs")
    suspend fun getActivityLog(@Query("date") date: String): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/steps")
    suspend fun updateSteps(@Body body: Map<String, Any>): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/water")
    suspend fun updateWater(@Body body: Map<String, Any>): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/sleep")
    suspend fun updateSleep(@Body body: Map<String, Any>): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/mood")
    suspend fun updateMood(@Body body: Map<String, Any>): Response<ApiResponse<ActivityLogDto>>

    @PATCH("activity-logs/note")
    suspend fun updateNote(@Body body: Map<String, Any>): Response<ApiResponse<ActivityLogDto>>

    @GET("activity-logs/range")
    suspend fun getActivityLogRange(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String
    ): Response<ApiResponse<List<ActivityLogDto>>>
}
