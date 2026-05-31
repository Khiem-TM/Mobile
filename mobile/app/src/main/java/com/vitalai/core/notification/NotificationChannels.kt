package com.vitalai.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Khai báo các NotificationChannel (Android 8+). Gọi 1 lần khi app khởi động. */
object NotificationChannels {
    const val SOCIAL = "vital_social"
    const val REMINDERS = "vital_reminders"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        nm.createNotificationChannel(
            NotificationChannel(
                SOCIAL,
                "Tương tác",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Lượt thích và bình luận trên bài viết của bạn" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                REMINDERS,
                "Nhắc nhở",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Nhắc cập nhật nhật ký ăn uống, hoạt động và buổi tập" }
        )
    }
}
