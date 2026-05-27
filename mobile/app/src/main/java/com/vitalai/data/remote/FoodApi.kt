package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.data.remote.model.FoodPageDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @GET("foods/explore")
    suspend fun exploreFoods(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null
    ): Response<ApiResponse<FoodPageDto>>

    @GET("foods/custom")
    suspend fun getCustomFoods(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<FoodPageDto>>

    @GET("foods/custom/{id}")
    suspend fun getCustomFoodById(@Path("id") id: String): Response<ApiResponse<FoodDto>>

    @GET("foods/favorites")
    suspend fun getFavorites(): Response<ApiResponse<List<FoodDto>>>

    @GET("foods/{id}")
    suspend fun getFoodById(@Path("id") id: String): Response<ApiResponse<FoodDto>>

    @GET("foods/barcode/{code}")
    suspend fun getFoodByBarcode(@Path("code") code: String): Response<ApiResponse<FoodDto>>

    @POST("foods")
    suspend fun createFood(@Body request: CreateFoodRequest): Response<ApiResponse<FoodDto>>

    @PATCH("foods/custom/{id}")
    suspend fun updateCustomFood(
        @Path("id") id: String,
        @Body request: CreateFoodRequest
    ): Response<ApiResponse<FoodDto>>

    @DELETE("foods/custom/{id}")
    suspend fun deleteCustomFood(@Path("id") id: String): Response<ApiResponse<Unit>>

    @POST("foods/{id}/favorite")
    suspend fun addFavorite(@Path("id") id: String): Response<ApiResponse<Unit>>

    @DELETE("foods/{id}/favorite")
    suspend fun removeFavorite(@Path("id") id: String): Response<ApiResponse<Unit>>
}
