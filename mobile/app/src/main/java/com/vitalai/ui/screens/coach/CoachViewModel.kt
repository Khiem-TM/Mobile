package com.vitalai.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ChatMessageDto
import com.vitalai.data.remote.model.ChatSessionDto
import com.vitalai.data.repository.ChatbotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachUiState(
    val sessions: List<ChatSessionDto> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<ChatMessageDto> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatbotRepository.getSessions().onSuccess { sessions ->
                val firstId = sessions.firstOrNull()?.id
                _uiState.update { it.copy(sessions = sessions, currentSessionId = firstId, isLoading = false) }
                if (firstId != null) loadMessages(firstId)
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadMessages(sessionId: String) {
        viewModelScope.launch {
            chatbotRepository.getMessages(sessionId).onSuccess { msgs ->
                _uiState.update { it.copy(messages = msgs, currentSessionId = sessionId) }
            }
        }
    }

    fun selectSession(sessionId: String) {
        if (_uiState.value.currentSessionId == sessionId) return
        _uiState.update { it.copy(currentSessionId = sessionId, messages = emptyList()) }
        loadMessages(sessionId)
    }

    fun createNewSession() {
        viewModelScope.launch {
            chatbotRepository.createSession(null).onSuccess { session ->
                _uiState.update { state ->
                    state.copy(
                        sessions = listOf(session) + state.sessions,
                        currentSessionId = session.id,
                        messages = emptyList()
                    )
                }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatbotRepository.deleteSession(sessionId).onSuccess {
                val remaining = _uiState.value.sessions.filter { it.id != sessionId }
                val wasActive = _uiState.value.currentSessionId == sessionId
                val newCurrentId = if (wasActive) remaining.firstOrNull()?.id else _uiState.value.currentSessionId
                _uiState.update { it.copy(sessions = remaining, currentSessionId = newCurrentId, messages = if (wasActive) emptyList() else it.messages) }
                if (wasActive && newCurrentId != null) loadMessages(newCurrentId)
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val sessionId = _uiState.value.currentSessionId ?: return
        val content = _uiState.value.inputText.trim()
        if (content.isBlank()) return

        val userMsg = ChatMessageDto(
            id = "temp_${System.currentTimeMillis()}",
            role = "user",
            content = content,
            createdAt = ""
        )

        _uiState.update { it.copy(messages = it.messages + userMsg, inputText = "", isSending = true) }

        viewModelScope.launch {
            chatbotRepository.sendMessage(sessionId, content).onSuccess { aiMsg ->
                // Update session's lastMessage preview in the list
                val updatedSessions = _uiState.value.sessions.map { s ->
                    if (s.id == sessionId) s.copy(lastMessage = aiMsg.content.take(80)) else s
                }
                _uiState.update { it.copy(messages = it.messages + aiMsg, isSending = false, sessions = updatedSessions) }
            }.onFailure {
                _uiState.update { it.copy(isSending = false, error = "Không gửi được tin nhắn. Thử lại.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
