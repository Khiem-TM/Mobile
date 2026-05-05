package com.vitalai.data.repository

import com.vitalai.data.remote.FoodApi
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.data.remote.model.FoodPageDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodApi: FoodApi
) {
    suspend fun searchFoods(query: String, page: Int = 1, limit: Int = 20): Result<FoodPageDto> {
        return try {
            val response = foodApi.getFoods(search = query, page = page, limit = limit)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tìm kiếm (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFavorites(): Result<List<FoodDto>> {
        return try {
            val response = foodApi.getFavorites()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFoodById(id: String): Result<FoodDto> {
        return try {
            val response = foodApi.getFoodById(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không tìm thấy món ăn"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFood(request: CreateFoodRequest): Result<FoodDto> {
        return try {
            val response = foodApi.createFood(request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tạo món ăn (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean): Result<Unit> {
        return try {
            val response = if (isFavorite) foodApi.addFavorite(id) else foodApi.removeFavorite(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi cập nhật yêu thích"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
