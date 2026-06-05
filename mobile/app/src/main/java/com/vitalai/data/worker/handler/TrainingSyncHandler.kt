package com.vitalai.data.worker.handler

import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.remote.TrainingApi
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingSyncHandler @Inject constructor(
    private val trainingApi: TrainingApi,
) : SyncActionHandler {

    override fun canHandle(actionType: String) =
        actionType.startsWith("UPDATE_STEPS") ||
        actionType.startsWith("UPDATE_WATER")

    override suspend fun handle(action: PendingSyncActionEntity): Boolean {
        return when {
            action.actionType.startsWith("UPDATE_STEPS") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                val steps = action.payload.toIntOrNull() ?: return false
                trainingApi.updateSteps(mapOf("logDate" to date, "steps" to steps)).isSuccessful
            }
            action.actionType.startsWith("UPDATE_WATER") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                val waterMl = action.payload.toIntOrNull() ?: return false
                trainingApi.updateWater(mapOf("logDate" to date, "waterMl" to waterMl)).isSuccessful
            }
            else -> false
        }
    }
}
