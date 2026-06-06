package com.vitalai.core.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.vitalai.core.logging.AppLogger
import com.vitalai.core.settings.AppSettingsDataStore
import com.vitalai.data.local.datastore.TokenManager
import com.vitalai.data.remote.DeviceTokenApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Đồng bộ FCM token với backend.
 * - Khi đã đăng nhập: POST token lên `devices/token`, xoá pending nếu thành công.
 * - Khi chưa đăng nhập: chỉ lưu pending để gửi sau khi login.
 */
@Singleton
class DeviceTokenRegistrar @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val deviceTokenApi: DeviceTokenApi,
    private val tokenManager: TokenManager,
    private val logger: AppLogger
) {
    /** Gọi từ FirebaseMessagingService.onNewToken. */
    suspend fun register(token: String) {
        appSettingsDataStore.savePendingFcmToken(token)
        sendToBackend(token)
    }

    /** Gọi sau khi đăng nhập / khi app khởi động (nếu đã đăng nhập). */
    suspend fun ensureRegistered() {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }
            .getOrElse {
                logger.debug(TAG, "Không lấy được FCM token: ${it.message}")
                appSettingsDataStore.pendingFcmToken.first()
            } ?: return
        sendToBackend(token)
    }

    /** Gỡ token khỏi backend + Firebase khi logout. */
    suspend fun unregisterCurrent() {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            ?: appSettingsDataStore.pendingFcmToken.first()
        if (token != null) {
            runCatching { deviceTokenApi.unregister(mapOf("token" to token)) }
        }
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
        appSettingsDataStore.clearPendingFcmToken()
    }

    private suspend fun sendToBackend(token: String) {
        val accessToken = tokenManager.accessToken.first()
        if (accessToken.isNullOrBlank()) {
            logger.debug(TAG, "Chưa đăng nhập -> giữ FCM token ở pending.")
            return
        }
        val ok = runCatching {
            deviceTokenApi.register(mapOf("token" to token, "platform" to "android"))
        }.map { it.isSuccessful }.getOrDefault(false)
        if (ok) {
            appSettingsDataStore.clearPendingFcmToken()
            logger.debug(TAG, "Đã đăng ký FCM token với backend.")
        } else {
            logger.debug(TAG, "Đăng ký FCM token thất bại -> giữ pending để thử lại.")
        }
    }

    private companion object {
        const val TAG = "DeviceTokenRegistrar"
    }
}
