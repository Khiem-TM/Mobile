package com.vitalai.data.worker.handler

import com.squareup.moshi.Moshi
import com.vitalai.data.local.room.dao.WorkoutSessionDao
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutSyncHandler @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val workoutSessionDao: WorkoutSessionDao,
    private val moshi: Moshi,
) : SyncActionHandler {

    override fun canHandle(actionType: String) = actionType.startsWith("CREATE_SESSION:")

    override suspend fun handle(action: PendingSyncActionEntity): Boolean {
        val request = moshi.adapter(CreateWorkoutSessionDto::class.java).fromJson(action.payload)
            ?: return true // payload hỏng → bỏ qua

        return try {
            // pushPendingSession reconcile cache theo ngày (xoá temp/trùng, giữ 1 card server).
            trainingRepository.pushPendingSession(request)
        } catch (e: retrofit2.HttpException) {
            if (e.code() in 400..499) {
                // Lỗi client vĩnh viễn → dọn bản optimistic của ngày đó, không retry.
                workoutSessionDao.deletePendingByDate(request.sessionDate)
                true
            } else {
                false // 5xx → retry
            }
        } catch (_: Exception) {
            false // network → retry
        }
    }
}
