package com.vitalai.core.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Đích điều hướng khi người dùng chạm vào thông báo push. */
data class NotificationDeepLink(val route: String, val id: String? = null)

/**
 * Cầu nối deep-link giữa MainActivity (nhận intent từ notification) và NavGraph.
 * Dùng StateFlow giữ pending để không mất event khi app cold-start từ notification.
 */
@Singleton
class DeepLinkBus @Inject constructor() {
    private val _pending = MutableStateFlow<NotificationDeepLink?>(null)
    val pending: StateFlow<NotificationDeepLink?> = _pending.asStateFlow()

    fun emit(link: NotificationDeepLink) {
        _pending.value = link
    }

    fun consume() {
        _pending.value = null
    }
}
