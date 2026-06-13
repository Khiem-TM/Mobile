package com.vitalai.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cầu nối báo cho UI khi session đã chết hẳn (refresh token cũng không còn dùng được),
 * để NavGraph tự điều hướng về Welcome ngay cả khi user không tự bấm Đăng xuất.
 */
@Singleton
class SessionExpiryBus @Inject constructor() {
    private val _expired = MutableStateFlow(false)
    val expired: StateFlow<Boolean> = _expired.asStateFlow()

    fun notifyExpired() {
        _expired.value = true
    }

    fun consume() {
        _expired.value = false
    }
}
