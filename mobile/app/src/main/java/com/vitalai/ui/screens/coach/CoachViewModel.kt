package com.vitalai.ui.screens.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ChatMessageDto
import com.vitalai.data.remote.model.ChatSessionDto
import com.vitalai.data.remote.model.ChatSourceDto
import com.vitalai.data.remote.model.ChatStreamEvent
import com.vitalai.data.repository.ChatbotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoachMessageUi(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: String,
    val isStreaming: Boolean = false,
    val sources: List<ChatSourceDto> = emptyList(),
    val intent: String? = null,
    val disclaimer: String? = null,
    val isError: Boolean = false
) {
    companion object {
        fun fromDto(dto: ChatMessageDto): CoachMessageUi = CoachMessageUi(
            id = dto.id,
            role = dto.role,
            content = dto.content,
            createdAt = dto.createdAt
        )
    }
}

data class CoachUiState(
    val sessions: List<ChatSessionDto> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<CoachMessageUi> = emptyList(),
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
                _uiState.update {
                    it.copy(
                        messages = msgs.map(CoachMessageUi::fromDto),
                        currentSessionId = sessionId
                    )
                }
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

        val now = System.currentTimeMillis()
        val userMsg = CoachMessageUi(
            id = "temp_user_$now",
            role = "user",
            content = content,
            createdAt = ""
        )
        val assistantTempId = "temp_assistant_$now"
        val assistantMsg = CoachMessageUi(
            id = assistantTempId,
            role = "assistant",
            content = "",
            createdAt = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + assistantMsg,
                inputText = "",
                isSending = true,
                error = null
            )
        }

        viewModelScope.launch {
            chatbotRepository.sendMessageStream(sessionId, content)
                .catch {
                    markAssistantError(assistantTempId, "Không gửi được tin nhắn. Thử lại.")
                }
                .collect { event ->
                    when (event) {
                        is ChatStreamEvent.Meta -> updateAssistant(assistantTempId) {
                            it.copy(intent = event.intent, sources = event.sources)
                        }
                        is ChatStreamEvent.Delta -> updateAssistant(assistantTempId) {
                            it.copy(content = it.content + event.text, isStreaming = true)
                        }
                        is ChatStreamEvent.Done -> {
                            val finalMessage = CoachMessageUi.fromDto(event.message).copy(
                                intent = event.intent,
                                sources = event.sources,
                                disclaimer = event.disclaimer,
                                isStreaming = false
                            )
                            val updatedSessions = _uiState.value.sessions.map { s ->
                                if (s.id == sessionId) s.copy(lastMessage = finalMessage.content.take(80)) else s
                            }
                            _uiState.update { state ->
                                state.copy(
                                    messages = state.messages.map {
                                        if (it.id == assistantTempId) finalMessage else it
                                    },
                                    isSending = false,
                                    sessions = updatedSessions
                                )
                            }
                        }
                        is ChatStreamEvent.Error -> markAssistantError(assistantTempId, event.message)
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun updateAssistant(
        assistantTempId: String,
        transform: (CoachMessageUi) -> CoachMessageUi
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == assistantTempId) transform(msg) else msg
                }
            )
        }
    }

    private fun markAssistantError(assistantTempId: String, message: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == assistantTempId) {
                        msg.copy(isStreaming = false, isError = true)
                    } else {
                        msg
                    }
                },
                isSending = false,
                error = message
            )
        }
    }
}
