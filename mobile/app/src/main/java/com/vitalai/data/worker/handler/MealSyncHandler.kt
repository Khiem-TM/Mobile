package com.vitalai.data.worker.handler

import com.squareup.moshi.Moshi
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.remote.MealLogApi
import com.vitalai.data.remote.model.AddMealItemRequest
import com.vitalai.data.remote.model.UpdateMealLogItemRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealSyncHandler @Inject constructor(
    private val mealLogApi: MealLogApi,
    private val moshi: Moshi,
) : SyncActionHandler {

    override fun canHandle(actionType: String) =
        actionType.startsWith("ADD_MEAL_ITEM:") ||
        actionType.startsWith("DELETE_MEAL_ITEM:") ||
        actionType.startsWith("UPDATE_MEAL_ITEM:")

    override suspend fun handle(action: PendingSyncActionEntity): Boolean {
        return when {
            action.actionType.startsWith("ADD_MEAL_ITEM:") -> {
                val mealLogId = action.actionType.removePrefix("ADD_MEAL_ITEM:")
                val request = moshi.adapter(AddMealItemRequest::class.java).fromJson(action.payload)
                    ?: return false
                mealLogApi.addItem(mealLogId, request).isSuccessful
            }
            action.actionType.startsWith("DELETE_MEAL_ITEM:") -> {
                val parts = action.actionType.removePrefix("DELETE_MEAL_ITEM:").split(":")
                if (parts.size != 2) return false
                mealLogApi.deleteItem(parts[0], parts[1]).isSuccessful
            }
            action.actionType.startsWith("UPDATE_MEAL_ITEM:") -> {
                val parts = action.actionType.removePrefix("UPDATE_MEAL_ITEM:").split(":")
                if (parts.size != 2) return true // malformed → discard
                val request = moshi.adapter(UpdateMealLogItemRequest::class.java).fromJson(action.payload)
                    ?: return false
                mealLogApi.updateItem(parts[0], parts[1], request).isSuccessful
            }
            else -> false
        }
    }
}
