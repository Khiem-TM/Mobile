package com.vitalai.core.network

import com.squareup.moshi.Moshi
import com.vitalai.data.remote.model.ChatStreamDeltaDto
import com.vitalai.data.remote.model.ChatStreamDoneDto
import com.vitalai.data.remote.model.ChatStreamErrorDto
import com.vitalai.data.remote.model.ChatStreamEvent
import com.vitalai.data.remote.model.ChatStreamMetaDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSseParser @Inject constructor(
    private val moshi: Moshi
) {
    private val metaAdapter = moshi.adapter(ChatStreamMetaDto::class.java)
    private val deltaAdapter = moshi.adapter(ChatStreamDeltaDto::class.java)
    private val doneAdapter = moshi.adapter(ChatStreamDoneDto::class.java)
    private val errorAdapter = moshi.adapter(ChatStreamErrorDto::class.java)

    fun parse(body: ResponseBody): Flow<ChatStreamEvent> = flow {
        body.use { responseBody ->
            val source = responseBody.source()
            var eventName = "message"
            val dataLines = mutableListOf<String>()

            fun reset() {
                eventName = "message"
                dataLines.clear()
            }

            fun decodeEvent(): ChatStreamEvent? {
                if (dataLines.isEmpty()) {
                    reset()
                    return null
                }
                val data = dataLines.joinToString("\n")
                val decoded = when (eventName) {
                    "meta" -> metaAdapter.fromJson(data)?.let {
                        ChatStreamEvent.Meta(
                            intent = it.intent ?: "smalltalk",
                            sources = it.sources
                        )
                    }
                    "delta" -> deltaAdapter.fromJson(data)?.text
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { ChatStreamEvent.Delta(it) }
                    "done" -> doneAdapter.fromJson(data)?.let {
                        ChatStreamEvent.Done(
                            message = it.message,
                            intent = it.intent ?: "smalltalk",
                            sources = it.sources,
                            disclaimer = it.disclaimer
                        )
                    }
                    "error" -> errorAdapter.fromJson(data)?.let {
                        ChatStreamEvent.Error(it.message ?: "Không thể kết nối trợ lý AI.")
                    }
                    else -> null
                }
                reset()
                return decoded
            }

            while (!source.exhausted()) {
                val rawLine = source.readUtf8Line() ?: break
                val line = rawLine.removeSuffix("\r")
                when {
                    line.isEmpty() -> decodeEvent()?.let { emit(it) }
                    line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> dataLines += line.removePrefix("data:").trimStart()
                }
            }
            decodeEvent()?.let { emit(it) }
        }
    }
}
