package com.vitalai.data.worker.handler

import com.squareup.moshi.Moshi
import com.vitalai.data.local.room.dao.BodyMetricDao
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.mapper.toEntity
import com.vitalai.data.remote.BodyMetricsApi
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyMetricSyncHandler @Inject constructor(
    private val bodyMetricsApi: BodyMetricsApi,
    private val bodyMetricDao: BodyMetricDao,
    private val moshi: Moshi,
) : SyncActionHandler {

    override fun canHandle(actionType: String) = actionType.startsWith("ADD_BODY_METRIC:")

    override suspend fun handle(action: PendingSyncActionEntity): Boolean {
        val tempId = action.actionType.removePrefix("ADD_BODY_METRIC:")
        val request = moshi.adapter(UpsertBodyMetricRequest::class.java).fromJson(action.payload)
            ?: return true // malformed → discard
        val response = bodyMetricsApi.addMetric(request)
        if (response.isSuccessful) {
            val dto = response.body()?.data
            if (dto != null) {
                bodyMetricDao.replaceEntity(tempId, dto.toEntity(isPendingSync = false))
            }
            return true
        }
        if (response.code() in 400..499) {
            // lỗi client vĩnh viễn — dọn temp, refreshFromNetwork sẽ import lại bản ghi server
            bodyMetricDao.deleteById(tempId)
            return true
        }
        return false // 5xx / network: WorkManager retry
    }
}
