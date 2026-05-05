package com.vitalai.data.repository

import com.vitalai.data.remote.MealLogApi
import com.vitalai.data.remote.model.AddMealItemRequest
import com.vitalai.data.remote.model.CreateMealLogRequest
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.MealLogItemDto
import com.vitalai.data.remote.model.MealLogSummaryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealLogRepository @Inject constructor(
    private val mealLogApi: MealLogApi
) {
    suspend fun createMealLog(mealType: String, date: String): Result<MealLogDto> {
        return try {
            val response = mealLogApi.createMealLog(CreateMealLogRequest(mealType, date))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tạo bữa ăn (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMealLogs(date: String): Result<List<MealLogDto>> {
        return try {
            val response = mealLogApi.getMealLogs(date)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMealSummary(date: String): Result<MealLogSummaryDto> {
        return try {
            val response = mealLogApi.getMealSummary(date)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải tổng kết (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addItem(mealLogId: String, request: AddMealItemRequest): Result<MealLogItemDto> {
        return try {
            val response = mealLogApi.addItem(mealLogId, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi thêm món (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(mealLogId: String, itemId: String): Result<Unit> {
        return try {
            val response = mealLogApi.deleteItem(mealLogId, itemId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xóa món (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
