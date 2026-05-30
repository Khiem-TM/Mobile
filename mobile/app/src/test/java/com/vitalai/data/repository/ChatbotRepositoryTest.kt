package com.vitalai.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vitalai.data.remote.ChatSseParser
import com.vitalai.data.remote.ChatbotApi
import com.vitalai.data.remote.model.ChatStreamEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class ChatbotRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ChatbotRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ChatbotApi::class.java)
        repository = ChatbotRepository(api, ChatSseParser(moshi))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `sendMessageStream emits upstream SSE events`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    """
                    event: meta
                    data: {"intent":"workout","sources":[]}

                    event: delta
                    data: {"text":"Tập "}

                    event: done
                    data: {"message":{"id":"m1","role":"assistant","content":"Tập ","created_at":"now"},"intent":"workout","sources":[]}

                    """.trimIndent()
                )
        )

        val events = repository.sendMessageStream("s1", "hello").toList()

        assertEquals(3, events.size)
        assertTrue(events[0] is ChatStreamEvent.Meta)
        assertTrue(events[1] is ChatStreamEvent.Delta)
        assertTrue(events[2] is ChatStreamEvent.Done)
        assertEquals("workout", (events.first() as ChatStreamEvent.Meta).intent)
        assertEquals("/chatbot/sessions/s1/messages/stream", server.takeRequest().path)
    }

    @Test
    fun `sendMessageStream falls back to sync when stream setup fails`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "statusCode": 201,
                      "data": {
                        "id": "m2",
                        "role": "assistant",
                        "content": "Fallback reply",
                        "created_at": "now"
                      }
                    }
                    """.trimIndent()
                )
        )

        val events = repository.sendMessageStream("s1", "hello").toList()

        assertTrue(events[0] is ChatStreamEvent.Delta)
        assertEquals("Fallback reply", (events[0] as ChatStreamEvent.Delta).text)
        assertEquals("Fallback reply", (events[1] as ChatStreamEvent.Done).message.content)
        assertEquals("/chatbot/sessions/s1/messages/stream", server.takeRequest().path)
        assertEquals("/chatbot/sessions/s1/messages", server.takeRequest().path)
    }
}
