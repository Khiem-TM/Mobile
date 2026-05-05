package com.vitalai.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.NotificationDto
import com.vitalai.data.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<NotificationDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationsRepository: NotificationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            notificationsRepository.getNotifications().onSuccess { list ->
                _uiState.update { it.copy(notifications = list, isLoading = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            notificationsRepository.markRead(id).onSuccess { updated ->
                _uiState.update { state ->
                    state.copy(notifications = state.notifications.map { n ->
                        if (n.id == id) updated else n
                    })
                }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            notificationsRepository.markAllRead().onSuccess {
                _uiState.update { state ->
                    state.copy(notifications = state.notifications.map { it.copy(isRead = true) })
                }
            }
        }
    }
}
