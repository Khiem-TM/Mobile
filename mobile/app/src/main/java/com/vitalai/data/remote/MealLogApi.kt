package com.vitalai.data.remote

import com.vitalai.data.remote.model.AddMealItemRequest
import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.CreateMealLogRequest
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.MealLogItemDto
import com.vitalai.data.remote.model.MealLogSummaryDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MealLogApi {
    @POST("meal-logs")
    suspend fun createMealLog(@Body request: CreateMealLogRequest): Response<ApiResponse<MealLogDto>>

    @GET("meal-logs")
    suspend fun getMealLogs(@Query("date") date: String): Response<ApiResponse<List<MealLogDto>>>

    @GET("meal-logs/summary")
    suspend fun getMealSummary(@Query("date") date: String): Response<ApiResponse<MealLogSummaryDto>>

    @POST("meal-logs/{mealLogId}/items")
    suspend fun addItem(
        @Path("mealLogId") mealLogId: String,
        @Body request: AddMealItemRequest
    ): Response<ApiResponse<MealLogItemDto>>

    @DELETE("meal-logs/{mealLogId}/items/{itemId}")
    suspend fun deleteItem(
        @Path("mealLogId") mealLogId: String,
        @Path("itemId") itemId: String
    ): Response<ApiResponse<Unit>>
}
