package com.vitalai.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.squareup.moshi.Moshi
import com.vitalai.data.local.dao.MealLogDao
import com.vitalai.data.local.dao.MealLogWithItems
import com.vitalai.data.local.dao.PendingSyncActionDao
import com.vitalai.data.local.entity.MealLogEntity
import com.vitalai.data.local.entity.MealLogItemEntity
import com.vitalai.data.local.entity.PendingSyncActionEntity
import com.vitalai.data.remote.MealLogApi
import com.vitalai.data.remote.model.AddMealItemRequest
import com.vitalai.data.remote.model.CreateMealLogRequest
import com.vitalai.data.remote.model.FoodBriefDto
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.MealLogItemDto
import com.vitalai.data.remote.model.MealLogSummaryDto
import com.vitalai.data.worker.SyncWorker
import com.vitalai.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealLogRepository @Inject constructor(
    private val mealLogApi: MealLogApi,
    private val mealLogDao: MealLogDao,
    private val pendingSyncActionDao: PendingSyncActionDao,
    private val moshi: Moshi,
    @ApplicationContext private val context: Context,
) {
    // ── Offline-First: emit cache → fetch API → update cache ──────────────────

    fun getMealLogsFlow(date: String): Flow<Resource<List<MealLogDto>>> = flow {
        // 1. Emit cached data immediately (if available)
        val cached = mealLogDao.observeMealLogsWithItems(date).first()
        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached.toDto()))
        }

        // 2. Fetch fresh data from API
        emit(Resource.Loading)
        try {
            val response = mealLogApi.getMealLogs(date)
            val dtos = response.body()?.data
            if (response.isSuccessful && dtos != null) {
                // 3. Persist to DB — the Flow above will auto-re-emit
                mealLogDao.deleteByDate(date)
                mealLogDao.insertLogs(dtos.map { it.toEntity() })
                mealLogDao.insertItems(dtos.flatMap { it.toItemEntities() })
                emit(Resource.Success(dtos))
            } else if (cached.isEmpty()) {
                emit(Resource.Error("Không tải được dữ liệu (${response.code()})"))
            }
        } catch (e: IOException) {
            // Network error — return cached data with error message
            val stale = mealLogDao.observeMealLogsWithItems(date).first()
            emit(Resource.Error("Đang ngoại tuyến — hiển thị dữ liệu đã lưu", stale.toDto()))
        }
    }

    // ── One-shot getters (kept for backward compatibility) ────────────────────

    suspend fun getMealLogs(date: String): Result<List<MealLogDto>> {
        return try {
            val response = mealLogApi.getMealLogs(date)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: IOException) {
            val cached = mealLogDao.observeMealLogsWithItems(date).first()
            Result.success(cached.toDto())
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

    suspend fun createMealLog(mealType: String, date: String): Result<MealLogDto> {
        return try {
            val response = mealLogApi.createMealLog(CreateMealLogRequest(mealType, date))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                mealLogDao.insertLogs(listOf(body.toEntity()))
                Result.success(body)
            } else {
                Result.failure(Exception("Lỗi tạo bữa ăn (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Sync-aware write operations ───────────────────────────────────────────

    suspend fun addItem(mealLogId: String, request: AddMealItemRequest): Result<MealLogItemDto> {
        return try {
            val response = mealLogApi.addItem(mealLogId, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                // Persist the new item locally
                mealLogDao.insertItems(listOf(body.toEntity(mealLogId)))
                Result.success(body)
            } else {
                Result.failure(Exception("Lỗi thêm món (${response.code()})"))
            }
        } catch (e: IOException) {
            // Network error — queue for later sync
            val payload = moshi.adapter(AddMealItemRequest::class.java).toJson(request)
            pendingSyncActionDao.insert(
                PendingSyncActionEntity(
                    actionType = "ADD_MEAL_ITEM:$mealLogId",
                    payload = payload
                )
            )
            scheduleSyncWorker()
            Result.failure(IOException("Đã lưu vào hàng chờ, sẽ đồng bộ khi có mạng"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(mealLogId: String, itemId: String): Result<Unit> {
        return try {
            val response = mealLogApi.deleteItem(mealLogId, itemId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xóa món (${response.code()})"))
        } catch (e: IOException) {
            pendingSyncActionDao.insert(
                PendingSyncActionEntity(
                    actionType = "DELETE_MEAL_ITEM:$mealLogId:$itemId",
                    payload = ""
                )
            )
            scheduleSyncWorker()
            Result.failure(IOException("Đã lưu vào hàng chờ, sẽ đồng bộ khi có mạng"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scheduleSyncWorker() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_pending_actions",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

// ── Mapping extensions ────────────────────────────────────────────────────────

private fun MealLogDto.toEntity() = MealLogEntity(
    id = id,
    mealType = mealType,
    date = date
)

private fun MealLogDto.toItemEntities(): List<MealLogItemEntity> = items.map { item ->
    MealLogItemEntity(
        id = item.id,
        mealLogId = id,
        foodId = item.foodId,
        foodName = item.foodName,
        imageUrl = item.imageUrl,
        quantity = item.quantity,
        servingUnit = item.servingUnit,
        calories = item.calories,
        carbsG = item.carbsG,
        proteinG = item.proteinG,
        fatG = item.fatG
    )
}

private fun MealLogItemDto.toEntity(mealLogId: String) = MealLogItemEntity(
    id = id,
    mealLogId = mealLogId,
    foodId = foodId,
    foodName = foodName,
    imageUrl = imageUrl,
    quantity = quantity,
    servingUnit = servingUnit,
    calories = calories,
    carbsG = carbsG,
    proteinG = proteinG,
    fatG = fatG
)

private fun List<MealLogWithItems>.toDto(): List<MealLogDto> = map { withItems ->
    MealLogDto(
        id = withItems.log.id,
        mealType = withItems.log.mealType,
        date = withItems.log.date,
        items = withItems.items.map { entity ->
            MealLogItemDto(
                id = entity.id,
                foodId = entity.foodId,
                food = FoodBriefDto(id = entity.foodId, name = entity.foodName, imageUrls = entity.imageUrl?.let { listOf(it) }),
                quantity = entity.quantity,
                servingUnit = entity.servingUnit,
                calories = entity.calories,
                carbsG = entity.carbsG,
                proteinG = entity.proteinG,
                fatG = entity.fatG
            )
        }
    )
}
