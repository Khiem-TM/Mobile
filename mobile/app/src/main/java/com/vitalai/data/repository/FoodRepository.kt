package com.vitalai.data.repository

import com.vitalai.data.local.room.dao.FoodCacheDao
import com.vitalai.data.mapper.toCacheEntity
import com.vitalai.data.mapper.toDto
import com.vitalai.data.remote.FoodApi
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.data.remote.model.FoodPageDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodApi: FoodApi,
    private val foodCacheDao: FoodCacheDao
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

    // ── Food cache ────────────────────────────────────────────────────────────

    fun observeFavorites(): Flow<List<FoodDto>> =
        foodCacheDao.observeFavorites().map { list -> list.map { it.toDto() } }

    fun observeCustomFoods(): Flow<List<FoodDto>> =
        foodCacheDao.observeCustomFoods().map { list -> list.map { it.toDto() } }

    suspend fun refreshFavorites() {
        runCatching { foodApi.getFavorites() }
            .onSuccess { res ->
                val foods = res.body()?.data ?: return@onSuccess
                foodCacheDao.clearFavoriteFlags()
                foodCacheDao.upsertAll(foods.map { it.toCacheEntity(isFavorite = true) })
            }
    }

    suspend fun refreshCustomFoods() {
        runCatching { foodApi.getCustomFoods(limit = 50) }
            .onSuccess { res ->
                val foods = res.body()?.data?.items ?: return@onSuccess
                val favoriteIds = foodCacheDao.getFavoriteIds().toSet()
                foodCacheDao.upsertAll(foods.map { it.toCacheEntity(isFavorite = it.id in favoriteIds) })
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
            if (response.isSuccessful && body != null) {
                cacheFood(body)
                Result.success(body)
            } else cachedFoodOrFailure(id) { Exception("Không tìm thấy món ăn") }
        } catch (e: Exception) {
            cachedFoodOrFailure(id) { e }
        }
    }

    suspend fun getCustomFoodById(id: String): Result<FoodDto> {
        return try {
            val response = foodApi.getCustomFoodById(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                cacheFood(body)
                Result.success(body)
            } else cachedFoodOrFailure(id) { Exception("Không tìm thấy món của tôi") }
        } catch (e: Exception) {
            cachedFoodOrFailure(id) { e }
        }
    }

    /** Ghi cache 1 món, giữ nguyên cờ favorite đang có. */
    private suspend fun cacheFood(food: FoodDto) {
        val fav = foodCacheDao.getById(food.id)?.isFavorite ?: false
        foodCacheDao.upsert(food.toCacheEntity(isFavorite = fav))
    }

    /** Offline/không tìm thấy: trả món từ cache nếu có, ngược lại [error]. */
    private suspend fun cachedFoodOrFailure(id: String, error: () -> Throwable): Result<FoodDto> =
        foodCacheDao.getById(id)?.toDto()?.let { Result.success(it) } ?: Result.failure(error())

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
            if (response.isSuccessful) {
                foodCacheDao.deleteById(id)
                Result.success(Unit)
            } else Result.failure(Exception("Lỗi xoá món ăn (${response.code()})"))
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
                if (uploaded != null) {
                    foodCacheDao.upsert(uploaded.toCacheEntity())
                    return Result.success(uploaded)
                }
            }
            foodCacheDao.upsert(body.toCacheEntity())
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
            if (response.isSuccessful) {
                foodCacheDao.setFavorite(id, isFavorite)
                Result.success(Unit)
            } else Result.failure(Exception("Lỗi cập nhật yêu thích"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
