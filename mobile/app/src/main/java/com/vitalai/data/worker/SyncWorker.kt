package com.vitalai.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.vitalai.data.local.room.dao.BodyMetricDao
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.mapper.toEntity
import com.vitalai.data.remote.BodyMetricsApi
import com.vitalai.data.remote.MealLogApi
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.model.AddMealItemRequest
import com.vitalai.data.remote.model.UpdateMealLogItemRequest
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingSyncActionDao: PendingSyncActionDao,
    private val mealLogApi: MealLogApi,
    private val trainingApi: TrainingApi,
    private val bodyMetricsApi: BodyMetricsApi,
    private val bodyMetricDao: BodyMetricDao,
    private val moshi: Moshi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val actions = pendingSyncActionDao.getAll()
        if (actions.isEmpty()) return Result.success()

        var anyFailed = false
        for (action in actions) {
            val ok = try {
                dispatchAction(action)
            } catch (e: Exception) {
                false
            }
            if (ok) {
                pendingSyncActionDao.delete(action)
            } else {
                val updated = action.copy(retryCount = action.retryCount + 1)
                pendingSyncActionDao.update(updated)
                anyFailed = true
            }
        }
        return if (anyFailed) Result.retry() else Result.success()
    }

    private suspend fun dispatchAction(action: PendingSyncActionEntity): Boolean {
        return when {
            action.actionType.startsWith("ADD_MEAL_ITEM:") -> {
                val mealLogId = action.actionType.removePrefix("ADD_MEAL_ITEM:")
                val adapter = moshi.adapter(AddMealItemRequest::class.java)
                val request = adapter.fromJson(action.payload) ?: return false
                val response = mealLogApi.addItem(mealLogId, request)
                response.isSuccessful
            }
            action.actionType.startsWith("DELETE_MEAL_ITEM:") -> {
                val parts = action.actionType.removePrefix("DELETE_MEAL_ITEM:").split(":")
                if (parts.size != 2) return false
                val response = mealLogApi.deleteItem(parts[0], parts[1])
                response.isSuccessful
            }
            action.actionType.startsWith("UPDATE_MEAL_ITEM:") -> {
                val parts = action.actionType.removePrefix("UPDATE_MEAL_ITEM:").split(":")
                if (parts.size != 2) return true // malformed -> discard
                val req = moshi.adapter(UpdateMealLogItemRequest::class.java).fromJson(action.payload)
                    ?: return false
                val response = mealLogApi.updateItem(parts[0], parts[1], req)
                response.isSuccessful
            }
            // "UPDATE_STEPS:<date>" (date trong actionType, value trong payload). Có nhánh
            // cũ "UPDATE_STEPS" (today) để tương thích ngược.
            action.actionType.startsWith("UPDATE_STEPS") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                val steps = action.payload.toIntOrNull() ?: return false
                val response = trainingApi.updateSteps(mapOf("logDate" to date, "steps" to steps))
                response.isSuccessful
            }
            action.actionType.startsWith("UPDATE_WATER") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                val waterMl = action.payload.toIntOrNull() ?: return false
                val response = trainingApi.updateWater(mapOf("logDate" to date, "waterMl" to waterMl))
                response.isSuccessful
            }
            action.actionType.startsWith("ADD_BODY_METRIC:") -> {
                val tempId = action.actionType.removePrefix("ADD_BODY_METRIC:")
                val request = moshi.adapter(UpsertBodyMetricRequest::class.java).fromJson(action.payload)
                    ?: return true // malformed → discard
                val res = bodyMetricsApi.addMetric(request)
                if (res.isSuccessful) {
                    val dto = res.body()?.data
                    if (dto != null) {
                        bodyMetricDao.replaceEntity(tempId, dto.toEntity(isPendingSync = false))
                    }
                    true
                } else false
            }
            else -> true // unknown action type — discard
        }
    }
}
