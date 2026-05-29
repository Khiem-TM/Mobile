package com.vitalai.core.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vitalai.core.logging.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VitalFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var deviceTokenRegistrar: DeviceTokenRegistrar
    @Inject lateinit var logger: AppLogger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            deviceTokenRegistrar.register(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        logger.debug(TAG, "Push message received from ${message.from ?: "unknown"}.")
        // TODO: Map backend notification payload to Android notification channels.
    }

    private companion object {
        const val TAG = "VitalFCMService"
    }
}
