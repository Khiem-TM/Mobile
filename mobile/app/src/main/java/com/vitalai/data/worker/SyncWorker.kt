package com.vitalai.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import com.vitalai.data.local.dao.PendingSyncActionDao
import com.vitalai.data.local.entity.PendingSyncActionEntity
import com.vitalai.data.remote.MealLogApi
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.model.AddMealItemRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingSyncActionDao: PendingSyncActionDao,
    private val mealLogApi: MealLogApi,
    private val trainingApi: TrainingApi,
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
            action.actionType == "UPDATE_STEPS" -> {
                val steps = action.payload.toIntOrNull() ?: return false
                val response = trainingApi.updateSteps(mapOf("steps" to steps))
                response.isSuccessful
            }
            action.actionType == "UPDATE_WATER" -> {
                val waterMl = action.payload.toIntOrNull() ?: return false
                val response = trainingApi.updateWater(mapOf("water_ml" to waterMl))
                response.isSuccessful
            }
            else -> true // unknown action type — discard
        }
    }
}
