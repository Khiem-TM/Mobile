package com.vitalai.data.worker.handler

import com.vitalai.data.local.room.entity.PendingSyncActionEntity

interface SyncActionHandler {
    fun canHandle(actionType: String): Boolean
    suspend fun handle(action: PendingSyncActionEntity): Boolean
}
