package com.vitalai.data.worker.handler

import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.UpdateMoodRequest
import com.vitalai.data.remote.UpdateNoteRequest
import com.vitalai.data.remote.UpdateSleepRequest
import com.vitalai.data.remote.UpdateStepsRequest
import com.vitalai.data.remote.UpdateWaterRequest
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingSyncHandler @Inject constructor(
    private val trainingApi: TrainingApi,
) : SyncActionHandler {

    override fun canHandle(actionType: String) =
        actionType.startsWith("UPDATE_STEPS") ||
        actionType.startsWith("UPDATE_WATER") ||
        actionType.startsWith("UPDATE_SLEEP") ||
        actionType.startsWith("UPDATE_MOOD") ||
        actionType.startsWith("UPDATE_NOTE")

    override suspend fun handle(action: PendingSyncActionEntity): Boolean {
        return when {
            action.actionType.startsWith("UPDATE_STEPS") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                val steps = action.payload.toIntOrNull() ?: return false
                trainingApi.updateSteps(UpdateStepsRequest(date, steps)).isSuccessful
            }
            action.actionType.startsWith("UPDATE_WATER") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                val waterMl = action.payload.toIntOrNull() ?: return false
                trainingApi.updateWater(UpdateWaterRequest(date, waterMl)).isSuccessful
            }
            action.actionType.startsWith("UPDATE_SLEEP") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                val sleepHours = action.payload.toFloatOrNull() ?: return false
                trainingApi.updateSleep(UpdateSleepRequest(date, sleepHours)).isSuccessful
            }
            action.actionType.startsWith("UPDATE_MOOD") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                trainingApi.updateMood(UpdateMoodRequest(date, action.payload)).isSuccessful
            }
            action.actionType.startsWith("UPDATE_NOTE") -> {
                val date = action.actionType.substringAfter(":", LocalDate.now().toString())
                trainingApi.updateNote(UpdateNoteRequest(date, action.payload)).isSuccessful
            }
            else -> false
        }
    }
}
