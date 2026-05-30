package com.vitalai.ui.screens.coach

import com.vitalai.data.remote.ChatSseParser
import com.vitalai.data.remote.ChatbotApi
import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.ChatMessageDto
import com.vitalai.data.remote.model.ChatSessionDto
import com.vitalai.data.remote.model.CreateChatSessionRequest
import com.vitalai.data.remote.model.SendMessageRequest
import com.vitalai.data.repository.ChatbotRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CoachViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: FakeChatbotApi
    private lateinit var viewModel: CoachViewModel

    @Before
    fun setUp() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        api = FakeChatbotApi()
        viewModel = CoachViewModel(ChatbotRepository(api, ChatSseParser(moshi)))
    }

    @After
    fun tearDown() {
        api.streamBody = ""
    }

    @Test
    fun `sendMessage appends user message and final streamed assistant message`() = runTest {
        api.streamBody = """
            event: meta
            data: {"intent":"nutrition","sources":[{"title":"Macro","document_id":"doc1","chunk_index":0}]}

            event: delta
            data: {"text":"Xin "}

            event: delta
            data: {"text":"chào"}

            event: done
            data: {"message":{"id":"a1","role":"assistant","content":"Xin chào","created_at":"now"},"intent":"nutrition","sources":[{"title":"Macro","document_id":"doc1","chunk_index":0}],"disclaimer":"Tham khảo"}

        """.trimIndent()

        advanceUntilIdle()
        viewModel.updateInput("hello")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSending)
        assertEquals(2, state.messages.size)
        assertEquals("hello", state.messages[0].content)
        assertEquals("Xin chào", state.messages[1].content)
        assertEquals("nutrition", state.messages[1].intent)
        assertEquals("Macro", state.messages[1].sources.first().title)
    }

    @Test
    fun `sendMessage marks assistant error when stream emits error after partial content`() = runTest {
        api.streamBody = """
            event: delta
            data: {"text":"Một phần"}

            event: error
            data: {"message":"failed"}

        """.trimIndent()

        advanceUntilIdle()
        viewModel.updateInput("hello")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSending)
        assertTrue(state.messages[1].isError)
        assertEquals("Một phần", state.messages[1].content)
    }
}

private class FakeChatbotApi : ChatbotApi {
    var streamBody: String = ""

    override suspend fun getSessions(): Response<ApiResponse<List<ChatSessionDto>>> {
        return Response.success(
            ApiResponse(
                success = true,
                statusCode = 200,
                data = listOf(
                    ChatSessionDto(
                        id = "s1",
                        title = "Chat",
                        createdAt = "now",
                        lastMessage = null
                    )
                )
            )
        )
    }

    override suspend fun createSession(
        request: CreateChatSessionRequest
    ): Response<ApiResponse<ChatSessionDto>> {
        return Response.success(
            ApiResponse(
                success = true,
                statusCode = 201,
                data = ChatSessionDto("s1", "Chat", "now", null)
            )
        )
    }

    override suspend fun getMessages(
        sessionId: String
    ): Response<ApiResponse<List<ChatMessageDto>>> {
        return Response.success(ApiResponse(true, 200, emptyList()))
    }

    override suspend fun deleteSession(sessionId: String): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun sendMessage(
        sessionId: String,
        request: SendMessageRequest
    ): Response<ApiResponse<ChatMessageDto>> {
        return Response.success(
            ApiResponse(
                true,
                200,
                ChatMessageDto("sync", "assistant", "Sync reply", "now")
            )
        )
    }

    override suspend fun streamMessage(
        sessionId: String,
        request: SendMessageRequest
    ): Response<ResponseBody> {
        return Response.success(streamBody.toResponseBody("text/event-stream".toMediaType()))
    }
}
