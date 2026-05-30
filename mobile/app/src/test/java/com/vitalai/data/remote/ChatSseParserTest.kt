package com.vitalai.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vitalai.data.remote.model.ChatStreamEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSseParserTest {
    private val parser = ChatSseParser(
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    )

    @Test
    fun `parses meta deltas and done events`() = runTest {
        val body = """
            event: meta
            data: {"intent":"nutrition","sources":[{"title":"Macro","document_id":"doc1","chunk_index":0}]}

            event: delta
            data: {"text":"Xin "}

            event: delta
            data: {"text":"chào"}

            event: done
            data: {"message":{"id":"m1","role":"assistant","content":"Xin chào","created_at":"now"},"intent":"nutrition","sources":[],"disclaimer":"Tham khảo"}

        """.trimIndent().toResponseBody("text/event-stream".toMediaType())

        val events = parser.parse(body).toList()

        assertEquals(4, events.size)
        assertTrue(events[0] is ChatStreamEvent.Meta)
        assertEquals("Xin ", (events[1] as ChatStreamEvent.Delta).text)
        assertEquals("Xin chào", (events[3] as ChatStreamEvent.Done).message.content)
    }

    @Test
    fun `parses multiline data as one json payload`() = runTest {
        val body = """
            event: delta
            data: {
            data: "text":"multi"
            data: }

        """.trimIndent().toResponseBody("text/event-stream".toMediaType())

        val events = parser.parse(body).toList()

        assertEquals(listOf(ChatStreamEvent.Delta("multi")), events)
    }

    @Test
    fun `parses error events`() = runTest {
        val body = """
            event: error
            data: {"message":"failed"}

        """.trimIndent().toResponseBody("text/event-stream".toMediaType())

        val events = parser.parse(body).toList()

        assertEquals(listOf(ChatStreamEvent.Error("failed")), events)
    }
}
