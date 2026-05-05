package com.vitalai.data.repository

import com.vitalai.data.remote.ChatbotApi
import com.vitalai.data.remote.model.ChatMessageDto
import com.vitalai.data.remote.model.ChatSessionDto
import com.vitalai.data.remote.model.CreateChatSessionRequest
import com.vitalai.data.remote.model.SendMessageRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatbotRepository @Inject constructor(
    private val chatbotApi: ChatbotApi
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
}
