package com.vitalai.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.worker.handler.BodyMetricSyncHandler
import com.vitalai.data.worker.handler.MealSyncHandler
import com.vitalai.data.worker.handler.SyncActionHandler
import com.vitalai.data.worker.handler.TrainingSyncHandler
import com.vitalai.data.worker.handler.WorkoutSyncHandler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingSyncActionDao: PendingSyncActionDao,
    mealSyncHandler: MealSyncHandler,
    trainingSyncHandler: TrainingSyncHandler,
    bodyMetricSyncHandler: BodyMetricSyncHandler,
    workoutSyncHandler: WorkoutSyncHandler,
) : CoroutineWorker(context, params) {

    private val handlers: List<SyncActionHandler> = listOf(
        mealSyncHandler,
        trainingSyncHandler,
        bodyMetricSyncHandler,
        workoutSyncHandler,
    )

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
        val handler = handlers.firstOrNull { it.canHandle(action.actionType) }
        return handler?.handle(action) ?: true // unknown action type → discard
    }
}
