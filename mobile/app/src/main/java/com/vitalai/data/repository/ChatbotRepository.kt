package com.vitalai.data.repository

import com.vitalai.data.remote.ChatSseParser
import com.vitalai.data.remote.ChatbotApi
import com.vitalai.data.remote.model.ChatMessageDto
import com.vitalai.data.remote.model.ChatSessionDto
import com.vitalai.data.remote.model.ChatStreamEvent
import com.vitalai.data.remote.model.CreateChatSessionRequest
import com.vitalai.data.remote.model.SendMessageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatbotRepository @Inject constructor(
    private val chatbotApi: ChatbotApi,
    private val chatSseParser: ChatSseParser
) {
    suspend fun getSessions(): Result<List<ChatSessionDto>> {
        return try {
            val response = chatbotApi.getSessions()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSession(title: String? = null): Result<ChatSessionDto> {
        return try {
            val response = chatbotApi.createSession(CreateChatSessionRequest(title))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tạo cuộc hội thoại (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(sessionId: String): Result<List<ChatMessageDto>> {
        return try {
            val response = chatbotApi.getMessages(sessionId)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return try {
            val response = chatbotApi.deleteSession(sessionId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xóa cuộc hội thoại (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(sessionId: String, content: String): Result<ChatMessageDto> {
        return try {
            val response = chatbotApi.sendMessage(sessionId, SendMessageRequest(content))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi gửi tin nhắn (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendMessageStream(sessionId: String, content: String): Flow<ChatStreamEvent> = flow {
        var receivedDelta = false
        try {
            val response = chatbotApi.streamMessage(sessionId, SendMessageRequest(content))
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                emitSyncFallback(sessionId, content)
                return@flow
            }

            chatSseParser.parse(body).collect { event ->
                if (event is ChatStreamEvent.Delta) receivedDelta = true
                emit(event)
            }
        } catch (e: Exception) {
            if (!receivedDelta) {
                emitSyncFallback(sessionId, content)
            } else {
                emit(ChatStreamEvent.Error("Kết nối bị gián đoạn. Nội dung đang hiển thị có thể chưa được lưu."))
            }
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<ChatStreamEvent>.emitSyncFallback(
        sessionId: String,
        content: String
    ) {
        sendMessage(sessionId, content)
            .onSuccess { message ->
                emit(ChatStreamEvent.Delta(message.content))
                emit(
                    ChatStreamEvent.Done(
                        message = message,
                        intent = "smalltalk",
                        sources = emptyList(),
                        disclaimer = null
                    )
                )
            }
            .onFailure {
                emit(ChatStreamEvent.Error("Không gửi được tin nhắn. Thử lại."))
            }
    }
}
