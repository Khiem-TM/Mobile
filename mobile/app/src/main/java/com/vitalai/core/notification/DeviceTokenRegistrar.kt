package com.vitalai.core.notification

import com.vitalai.core.logging.AppLogger
import com.vitalai.core.settings.AppSettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTokenRegistrar @Inject constructor(
    private val appSettingsDataStore: AppSettingsDataStore,
    private val logger: AppLogger
) {
    suspend fun register(token: String) {
        appSettingsDataStore.savePendingFcmToken(token)
        logger.debug(TAG, "FCM token saved locally for backend registration.")
        // TODO: Send token to backend when a device-token endpoint is defined.
    }

    private companion object {
        const val TAG = "DeviceTokenRegistrar"
    }
}
