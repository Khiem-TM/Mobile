package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.ChatMessageDto
import com.vitalai.data.remote.model.ChatSessionDto
import com.vitalai.data.remote.model.CreateChatSessionRequest
import com.vitalai.data.remote.model.SendMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatbotApi {
    @GET("chatbot/sessions")
    suspend fun getSessions(): Response<ApiResponse<List<ChatSessionDto>>>

    @POST("chatbot/sessions")
    suspend fun createSession(@Body request: CreateChatSessionRequest): Response<ApiResponse<ChatSessionDto>>

    @GET("chatbot/sessions/{sessionId}/messages")
    suspend fun getMessages(@Path("sessionId") sessionId: String): Response<ApiResponse<List<ChatMessageDto>>>

    @POST("chatbot/sessions/{sessionId}/messages")
    suspend fun sendMessage(
        @Path("sessionId") sessionId: String,
        @Body request: SendMessageRequest
    ): Response<ApiResponse<ChatMessageDto>>
}
