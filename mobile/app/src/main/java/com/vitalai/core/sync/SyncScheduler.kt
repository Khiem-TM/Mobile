package com.vitalai.core.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vitalai.data.worker.SyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Điểm DUY NHẤT để kích hoạt đồng bộ hàng đợi (PendingSyncAction) lên backend.
 * Gọi sau mỗi lần enqueue 1 hành động ghi, và 1 lần khi app khởi động để vét hàng tồn.
 *
 * Dùng unique work (KEEP) + ràng buộc NetworkType.CONNECTED + backoff lũy thừa:
 * - Ghi khi offline -> hành động nằm trong Room, worker tự chạy lại khi có mạng.
 * - Nhiều lần gọi liên tiếp -> gộp về 1 lần chạy (KEEP) vì worker xử lý cả hàng đợi.
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) {
    fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val UNIQUE_NAME = "sync_pending_actions"
    }
}
