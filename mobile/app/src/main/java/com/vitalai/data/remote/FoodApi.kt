package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.data.remote.model.FoodPageDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FoodApi {
    @GET("foods")
    suspend fun getFoods(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<FoodPageDto>>

    @GET("foods/favorites")
    suspend fun getFavorites(): Response<ApiResponse<List<FoodDto>>>

    @GET("foods/{id}")
    suspend fun getFoodById(@Path("id") id: String): Response<ApiResponse<FoodDto>>

    @GET("foods/barcode/{code}")
    suspend fun getFoodByBarcode(@Path("code") code: String): Response<ApiResponse<FoodDto>>

    @POST("foods")
    suspend fun createFood(@Body request: CreateFoodRequest): Response<ApiResponse<FoodDto>>

    @POST("foods/{id}/favorite")
    suspend fun addFavorite(@Path("id") id: String): Response<ApiResponse<Unit>>

    @DELETE("foods/{id}/favorite")
    suspend fun removeFavorite(@Path("id") id: String): Response<ApiResponse<Unit>>
}
