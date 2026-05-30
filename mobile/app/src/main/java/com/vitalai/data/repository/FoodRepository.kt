package com.vitalai.data.repository

import com.vitalai.data.remote.FoodApi
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.data.remote.model.FoodPageDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
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

    suspend fun exploreFoods(page: Int = 1, limit: Int = 20, category: String? = null): Result<FoodPageDto> {
        return try {
            val response = foodApi.exploreFoods(page, limit, category)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải món khám phá (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomFoods(search: String? = null, page: Int = 1, limit: Int = 20): Result<FoodPageDto> {
        return try {
            val response = foodApi.getCustomFoods(search, page, limit)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải món của tôi (${response.code()})"))
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

    suspend fun getCustomFoodById(id: String): Result<FoodDto> {
        return try {
            val response = foodApi.getCustomFoodById(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không tìm thấy món của tôi"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomFood(id: String, request: CreateFoodRequest): Result<FoodDto> {
        return try {
            val response = foodApi.updateCustomFood(id, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi cập nhật món ăn (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomFood(id: String): Result<Unit> {
        return try {
            val response = foodApi.deleteCustomFood(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xoá món ăn (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFood(
        request: CreateFoodRequest,
        imageFile: File? = null,
        imageMimeType: String? = null
    ): Result<FoodDto> {
        return try {
            val response = foodApi.createFood(request)
            val body = response.body()?.data
            if (!response.isSuccessful || body == null) {
                val message = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: "Lỗi tạo món ăn (${response.code()})"
                return Result.failure(Exception(message))
            }
            // Food created. Upload the optional image as a second step; the food
            // still exists even if the image upload fails, so surface but don't lose it.
            if (imageFile != null) {
                val uploaded = uploadFoodImage(body.id, imageFile, imageMimeType ?: "image/jpeg").getOrNull()
                if (uploaded != null) return Result.success(uploaded)
            }
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFoodImage(id: String, file: File, mimeType: String): Result<FoodDto> {
        return try {
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = file.name,
                body = file.asRequestBody(mimeType.toMediaTypeOrNull())
            )
            val response = foodApi.uploadFoodImage(id, part)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải ảnh món ăn (${response.code()})"))
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
