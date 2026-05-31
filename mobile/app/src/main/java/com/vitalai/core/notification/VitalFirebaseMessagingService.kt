package com.vitalai.core.notification

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vitalai.MainActivity
import com.vitalai.R
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
        val data = message.data
        val route = data["route"]
        val id = data["id"]
        val title = message.notification?.title ?: data["title"] ?: "VitalAI"
        val body = message.notification?.body ?: data["body"].orEmpty()

        val channelId =
            if (route == "home") NotificationChannels.REMINDERS else NotificationChannels.SOCIAL
        showNotification(title, body, route, id, channelId)
    }

    private fun showNotification(
        title: String,
        body: String,
        route: String?,
        id: String?,
        channelId: String,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            route?.let { putExtra(EXTRA_ROUTE, it) }
            id?.let { putExtra(EXTRA_ID, it) }
        }
        val notificationId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.app_icon4)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Thiếu quyền POST_NOTIFICATIONS (Android 13+) -> bỏ qua, không crash.
            logger.debug(TAG, "Không thể hiển thị notification: ${e.message}")
        }
    }

    companion object {
        const val EXTRA_ROUTE = "vital_route"
        const val EXTRA_ID = "vital_id"
        private const val TAG = "VitalFCMService"
    }
}
