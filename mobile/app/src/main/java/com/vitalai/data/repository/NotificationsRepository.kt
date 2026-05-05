package com.vitalai.data.repository

import com.vitalai.data.remote.NotificationsApi
import com.vitalai.data.remote.model.NotificationDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val notificationsApi: NotificationsApi
) {
    suspend fun getNotifications(): Result<List<NotificationDto>> {
        return try {
            val response = notificationsApi.getNotifications()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markRead(id: String): Result<NotificationDto> {
        return try {
            val response = notificationsApi.markRead(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi cập nhật thông báo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllRead(): Result<Unit> {
        return try {
            val response = notificationsApi.markAllRead()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi cập nhật thông báo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
