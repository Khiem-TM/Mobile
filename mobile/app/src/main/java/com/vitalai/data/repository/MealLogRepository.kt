package com.vitalai.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.squareup.moshi.Moshi
import com.vitalai.core.sync.SyncScheduler
import com.vitalai.data.local.room.dao.MealLogDao
import com.vitalai.data.local.room.dao.MealLogWithItems
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.entity.MealLogEntity
import com.vitalai.data.local.room.entity.MealLogItemEntity
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.remote.MealLogApi
import com.vitalai.data.remote.model.AddMealItemRequest
import com.vitalai.data.remote.model.CreateMealLogRequest
import com.vitalai.data.remote.model.FoodBriefDto
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.MealLogItemDto
import com.vitalai.data.remote.model.MealLogSummaryDto
import com.vitalai.data.remote.model.UpdateMealLogItemRequest
import com.vitalai.data.worker.SyncWorker
import com.vitalai.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealLogRepository @Inject constructor(
    private val mealLogApi: MealLogApi,
    private val mealLogDao: MealLogDao,
    private val pendingSyncActionDao: PendingSyncActionDao,
    private val syncScheduler: SyncScheduler,
    private val moshi: Moshi,
    @ApplicationContext private val context: Context,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Live source-of-truth: UI observe Room, ghi vào Room là tự cập nhật ─────

    /** Dòng dữ liệu sống theo ngày (Room). Ghi optimistic vào Room sẽ tự phát lại. */
    fun observeMealLogs(date: String): Flow<List<MealLogDto>> =
        mealLogDao.observeMealLogsWithItems(date).map { it.toDto() }

    /** Làm mới Room từ network (chạy nền, KHÔNG nằm trong luồng ghi). */
    suspend fun refreshMealLogs(date: String): Result<Unit> {
        return try {
            val response = mealLogApi.getMealLogs(date)
            val dtos = response.body()?.data
            if (response.isSuccessful && dtos != null) {
                mealLogDao.deleteByDate(date)
                mealLogDao.insertLogs(dtos.map { it.toEntity() })
                mealLogDao.insertItems(dtos.flatMap { it.toItemEntities() })
                Result.success(Unit)
            } else Result.failure(Exception("Lỗi tải bữa ăn (${response.code()})"))
        } catch (e: IOException) {
            Result.success(Unit) // offline -> giữ cache Room hiện có
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    suspend fun getMealHistory(fromDate: String, toDate: String): Result<List<MealLogDto>> {
        return try {
            val response = mealLogApi.getMealHistory(fromDate, toDate)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải lịch sử bữa ăn (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Tìm bữa ăn theo loại từ Room (không gọi network) để thêm món tức thì. */
    suspend fun getCachedMealLog(date: String, mealType: String): MealLogDto? =
        mealLogDao.observeMealLogsWithItems(date).first().toDto().firstOrNull { it.mealType == mealType }

    /** Ước lượng macro snapshot phía client (sync sẽ chỉnh lại chính xác). */
    private fun buildOptimisticItem(food: FoodDto, quantity: Float, servingUnit: String): MealLogItemDto {
        val grams = if (servingUnit.lowercase() in setOf("g", "gram", "grams", "gam")) quantity
        else quantity * food.servingSizeG
        val f = grams / 100f
        return MealLogItemDto(
            id = "temp_" + UUID.randomUUID().toString(),
            foodId = food.id,
            food = FoodBriefDto(id = food.id, name = food.name, imageUrls = food.imageUrls),
            quantity = quantity,
            servingUnit = servingUnit,
            calories = food.caloriesPer100g * f,
            carbsG = food.carbsPer100g * f,
            proteinG = food.proteinPer100g * f,
            fatG = food.fatPer100g * f
        )
    }

    fun addFoodToMealAsync(mealType: String, date: String, food: FoodDto, quantity: Float, servingUnit: String) {
        repoScope.launch {
            delay(200) // Wait for exit animation to complete
            var mealLog = getCachedMealLog(date, mealType)
            if (mealLog == null) {
                mealLog = createMealLog(mealType, date).getOrNull() ?: return@launch
            }
            addItemOptimistic(mealLog.id, food, quantity, servingUnit)
        }
    }

    fun addFoodIdToMealAsync(mealType: String, date: String, foodId: String, quantity: Float, servingUnit: String) {
        repoScope.launch {
            delay(200) // Wait for exit animation to complete
            var mealLog = getCachedMealLog(date, mealType)
            if (mealLog == null) {
                mealLog = createMealLog(mealType, date).getOrNull() ?: return@launch
            }
            addItem(mealLog.id, AddMealItemRequest(foodId, quantity, servingUnit))
        }
    }

    /**
     * Thêm món optimistic: chèn món tạm (macro ước lượng) vào Room NGAY -> UI hiện liền,
     * rồi network reconcile sang bản chính xác (hoặc enqueue khi offline).
     */
    suspend fun addItemOptimistic(
        mealLogId: String, food: FoodDto, quantity: Float, servingUnit: String
    ): Result<Unit> {
        val temp = buildOptimisticItem(food, quantity, servingUnit)
        mealLogDao.insertItems(listOf(temp.toEntity(mealLogId)))
        val request = AddMealItemRequest(food.id, quantity, servingUnit)
        return try {
            val response = mealLogApi.addItem(mealLogId, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                mealLogDao.deleteItemById(temp.id)               // bỏ món tạm
                val finalEntity = body.toEntity(mealLogId).let {
                    it.copy(
                        foodName = it.foodName.ifBlank { temp.foodName },
                        imageUrl = it.imageUrl ?: temp.imageUrl
                    )
                }
                mealLogDao.insertItems(listOf(finalEntity)) // gắn bản thật
                Result.success(Unit)
            } else Result.failure(Exception("Lỗi thêm món (${response.code()})"))
        } catch (e: IOException) {
            pendingSyncActionDao.insert(
                PendingSyncActionEntity(
                    actionType = "ADD_MEAL_ITEM:$mealLogId",
                    payload = moshi.adapter(AddMealItemRequest::class.java).toJson(request)
                )
            )
            syncScheduler.requestSync()
            Result.success(Unit) // giữ món tạm; refresh sau khi có mạng sẽ thay bằng bản thật
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

    /**
     * Sửa optimistic: scale macro theo tỉ lệ số lượng vào Room NGAY (UI đổi tức thì qua Flow),
     * rồi đồng bộ network để lấy số chính xác (hoặc enqueue khi offline).
     */
    suspend fun updateItem(mealLogId: String, itemId: String, request: UpdateMealLogItemRequest): Result<MealLogItemDto> {
        // 1) Optimistic: scale theo tỉ lệ quantity (đúng khi giữ nguyên đơn vị; sync sẽ chỉnh lại).
        val old = mealLogDao.getItemById(itemId)
        val newQty = request.quantity
        if (old != null && newQty != null && old.quantity > 0f) {
            val f = newQty / old.quantity
            mealLogDao.insertItems(
                listOf(
                    old.copy(
                        quantity = newQty,
                        servingUnit = request.servingUnit ?: old.servingUnit,
                        calories = old.calories * f,
                        carbsG = old.carbsG * f,
                        proteinG = old.proteinG * f,
                        fatG = old.fatG * f
                    )
                )
            )
        }
        // 2) Đồng bộ network (reconcile số chính xác) hoặc enqueue khi offline.
        return try {
            val response = mealLogApi.updateItem(mealLogId, itemId, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                val finalEntity = body.toEntity(mealLogId).let {
                    it.copy(
                        foodName = it.foodName.ifBlank { old?.foodName ?: "" },
                        imageUrl = it.imageUrl ?: old?.imageUrl
                    )
                }
                mealLogDao.insertItems(listOf(finalEntity))
                Result.success(body)
            } else {
                Result.failure(Exception("Lỗi cập nhật món (${response.code()})"))
            }
        } catch (e: IOException) {
            pendingSyncActionDao.insert(
                PendingSyncActionEntity(
                    actionType = "UPDATE_MEAL_ITEM:$mealLogId:$itemId",
                    payload = moshi.adapter(UpdateMealLogItemRequest::class.java).toJson(request)
                )
            )
            syncScheduler.requestSync()
            Result.failure(IOException("Đã lưu, sẽ đồng bộ khi có mạng"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa optimistic: xóa khỏi Room NGAY (UI tự cập nhật qua Flow) + đưa vào hàng đợi,
     * SyncWorker gọi API xóa nền và tự retry. Không await network -> không giật.
     */
    suspend fun deleteItem(mealLogId: String, itemId: String): Result<Unit> {
        mealLogDao.deleteItemById(itemId)
        return try {
            val response = mealLogApi.deleteItem(mealLogId, itemId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Lỗi xóa món (${response.code()})"))
            }
        } catch (e: IOException) {
            pendingSyncActionDao.insert(
                PendingSyncActionEntity(
                    actionType = "DELETE_MEAL_ITEM:$mealLogId:$itemId",
                    payload = ""
                )
            )
            syncScheduler.requestSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMealLog(mealLogId: String): Result<Unit> {
        return try {
            val response = mealLogApi.deleteMealLog(mealLogId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xóa bữa ăn (${response.code()})"))
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
