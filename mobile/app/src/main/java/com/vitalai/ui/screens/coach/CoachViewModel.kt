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
                _uiState.update { it.copy(messages = it.messages + aiMsg, isSending = false) }
            }.onFailure {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }
}
