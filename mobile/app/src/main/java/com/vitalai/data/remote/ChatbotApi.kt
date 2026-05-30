package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.ChatMessageDto
import com.vitalai.data.remote.model.ChatSessionDto
import com.vitalai.data.remote.model.CreateChatSessionRequest
import com.vitalai.data.remote.model.SendMessageRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ChatbotApi {
    @GET("chatbot/sessions")
    suspend fun getSessions(): Response<ApiResponse<List<ChatSessionDto>>>

    @POST("chatbot/sessions")
    suspend fun createSession(@Body request: CreateChatSessionRequest): Response<ApiResponse<ChatSessionDto>>

    @GET("chatbot/sessions/{sessionId}/messages")
    suspend fun getMessages(@Path("sessionId") sessionId: String): Response<ApiResponse<List<ChatMessageDto>>>

    @DELETE("chatbot/sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<Unit>

    @POST("chatbot/sessions/{sessionId}/messages")
    suspend fun sendMessage(
        @Path("sessionId") sessionId: String,
        @Body request: SendMessageRequest
    ): Response<ApiResponse<ChatMessageDto>>

    @Streaming
    @POST("chatbot/sessions/{sessionId}/messages/stream")
    suspend fun streamMessage(
        @Path("sessionId") sessionId: String,
        @Body request: SendMessageRequest
    ): Response<ResponseBody>
}
