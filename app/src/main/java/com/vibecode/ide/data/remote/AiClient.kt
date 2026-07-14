package com.vibecode.ide.data.remote

import com.vibecode.ide.data.remote.dto.AnthropicMessage
import com.vibecode.ide.data.remote.dto.AnthropicRequest
import com.vibecode.ide.data.remote.dto.AnthropicResponse
import com.vibecode.ide.data.remote.dto.OaChatMessage
import com.vibecode.ide.data.remote.dto.OaChatRequest
import com.vibecode.ide.data.remote.dto.OaChatResponse
import com.vibecode.ide.data.remote.dto.OaModelsListResponse
import com.vibecode.ide.domain.model.AiModel
import com.vibecode.ide.domain.model.AiProvider
import com.vibecode.ide.domain.model.AuthMethod
import com.vibecode.ide.domain.model.ChatMessage
import com.vibecode.ide.domain.model.MessageRole
import com.vibecode.ide.domain.model.ModelSource
import com.vibecode.ide.domain.model.RequestFormat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider-agnostic HTTP client. Speaks OpenAI-compatible chat/completions by
 * default, with an Anthropic-format mode for providers that need it. Any new
 * "future provider" that is OpenAI-compatible works with zero code changes —
 * the user just configures a base URL, auth method, and model id.
 */
@Singleton
class AiClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun authorizedRequestBuilder(provider: AiProvider, url: String): Request.Builder {
        val httpUrl = url.toHttpUrlOrNull()?.newBuilder()
        var builder = Request.Builder()
        val key = provider.apiKey.orEmpty()

        val finalUrl = when {
            provider.authMethod == AuthMethod.QUERY_PARAM && key.isNotBlank() && httpUrl != null -> {
                httpUrl.addQueryParameter("key", key).build().toString()
            }
            else -> url
        }
        builder = builder.url(finalUrl)

        if (key.isNotBlank()) {
            when (provider.authMethod) {
                AuthMethod.BEARER -> builder.addHeader(provider.authHeaderName, "Bearer $key")
                AuthMethod.API_KEY_HEADER -> builder.addHeader(provider.authHeaderName, key)
                AuthMethod.QUERY_PARAM, AuthMethod.NONE -> {}
            }
        }
        provider.extraHeaders.forEach { (k, v) -> builder.addHeader(k, v) }
        builder.addHeader("Content-Type", "application/json")
        return builder
    }

    private fun toOpenAiMessages(messages: List<ChatMessage>, systemPrompt: String?): List<OaChatMessage> {
        val list = mutableListOf<OaChatMessage>()
        if (!systemPrompt.isNullOrBlank()) list.add(OaChatMessage("system", systemPrompt))
        messages.forEach {
            val role = when (it.role) {
                MessageRole.SYSTEM -> "system"
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.TOOL -> "user" // tool results are folded back in as user context
            }
            list.add(OaChatMessage(role, it.content))
        }
        return list
    }

    private fun toAnthropicMessages(messages: List<ChatMessage>): List<AnthropicMessage> =
        messages.filter { it.role != MessageRole.SYSTEM }.map {
            val role = if (it.role == MessageRole.ASSISTANT) "assistant" else "user"
            AnthropicMessage(role, it.content)
        }

    /** Streams a chat completion. Emits Delta events as tokens arrive, then a Done event. */
    fun streamChat(
        provider: AiProvider,
        modelId: String,
        history: List<ChatMessage>,
        systemPrompt: String?,
        maxTokens: Int = 4096,
    ): Flow<StreamEvent> = callbackFlow {
        val bodyJson: String
        val url: String

        when (provider.requestFormat) {
            RequestFormat.ANTHROPIC -> {
                url = provider.chatCompletionsUrl
                val req = AnthropicRequest(
                    model = modelId,
                    messages = toAnthropicMessages(history),
                    system = systemPrompt,
                    stream = true,
                    maxTokens = maxTokens,
                )
                bodyJson = json.encodeToString(AnthropicRequest.serializer(), req)
            }
            else -> {
                url = provider.chatCompletionsUrl
                val req = OaChatRequest(
                    model = modelId,
                    messages = toOpenAiMessages(history, systemPrompt),
                    stream = true,
                    maxTokens = maxTokens,
                )
                bodyJson = json.encodeToString(OaChatRequest.serializer(), req)
            }
        }

        val request = authorizedRequestBuilder(provider, url)
            .addHeader("Accept", "text/event-stream")
            .post(bodyJson.toRequestBody(jsonMedia))
            .build()

        var anthropicStopReason: String? = null

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Done(null))
                    return
                }
                try {
                    if (provider.requestFormat == RequestFormat.ANTHROPIC) {
                        val evt = json.decodeFromString(com.vibecode.ide.data.remote.dto.AnthropicStreamEvent.serializer(), data)
                        when (evt.type) {
                            "content_block_delta" -> evt.delta?.text?.let { trySend(StreamEvent.Delta(it)) }
                            // stop_reason (e.g. "max_tokens") arrives here, one event before message_stop.
                            "message_delta" -> evt.delta?.stopReason?.let { anthropicStopReason = it }
                            "message_stop" -> trySend(StreamEvent.Done(anthropicStopReason))
                        }
                    } else {
                        val chunk = json.decodeFromString(com.vibecode.ide.data.remote.dto.OaStreamChunk.serializer(), data)
                        val choice = chunk.choices.firstOrNull()
                        choice?.delta?.content?.let { trySend(StreamEvent.Delta(it)) }
                        if (choice?.finishReason != null) trySend(StreamEvent.Done(choice.finishReason))
                    }
                } catch (e: Exception) {
                    // Some providers send heartbeat/comment lines that aren't valid JSON — ignore them.
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (response != null && response.isSuccessful) {
                    // Some OpenAI-compatible providers (e.g. some Groq responses) ignore
                    // stream=true and return one complete JSON body instead of SSE chunks.
                    // okhttp-sse treats a non "text/event-stream" body as a failure — recover
                    // by parsing it as a normal chat-completion response instead of erroring.
                    val bodyText = runCatching { response.body?.string() }.getOrNull()
                    val fallbackText = bodyText?.let { text ->
                        runCatching {
                            if (provider.requestFormat == RequestFormat.ANTHROPIC) {
                                json.decodeFromString(AnthropicResponse.serializer(), text)
                                    .content.joinToString("") { it.text.orEmpty() }
                            } else {
                                json.decodeFromString(OaChatResponse.serializer(), text)
                                    .choices.firstOrNull()?.message?.content.orEmpty()
                            }
                        }.getOrNull()
                    }
                    if (!fallbackText.isNullOrBlank()) {
                        trySend(StreamEvent.Delta(fallbackText))
                        trySend(StreamEvent.Done(null))
                        close()
                        return
                    }
                    trySend(StreamEvent.Error("HTTP ${response.code}: ${bodyText ?: response.message}", t))
                    close()
                    return
                }
                val msg = response?.let { "HTTP ${it.code}: ${runCatching { it.body?.string() }.getOrNull() ?: it.message}" }
                    ?: (t?.message ?: "Unknown streaming error")
                trySend(StreamEvent.Error(msg, t))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val factory = EventSources.createFactory(okHttpClient)
        val eventSource = factory.newEventSource(request, listener)

        awaitClose { eventSource.cancel() }
    }

    /** Non-streaming fallback, used when a model/provider doesn't support SSE streaming. */
    suspend fun chatOnce(
        provider: AiProvider,
        modelId: String,
        history: List<ChatMessage>,
        systemPrompt: String?,
        maxTokens: Int = 4096,
    ): Result<String> {
        return try {
            val bodyJson: String
            when (provider.requestFormat) {
                RequestFormat.ANTHROPIC -> {
                    val req = AnthropicRequest(
                        model = modelId, messages = toAnthropicMessages(history),
                        system = systemPrompt, stream = false, maxTokens = maxTokens,
                    )
                    bodyJson = json.encodeToString(AnthropicRequest.serializer(), req)
                }
                else -> {
                    val req = OaChatRequest(
                        model = modelId, messages = toOpenAiMessages(history, systemPrompt), stream = false,
                        maxTokens = maxTokens,
                    )
                    bodyJson = json.encodeToString(OaChatRequest.serializer(), req)
                }
            }
            val request = authorizedRequestBuilder(provider, provider.chatCompletionsUrl)
                .post(bodyJson.toRequestBody(jsonMedia))
                .build()

            okHttpClient.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return Result.failure(IOException("HTTP ${resp.code}: $text"))
                if (provider.requestFormat == RequestFormat.ANTHROPIC) {
                    val parsed = json.decodeFromString(AnthropicResponse.serializer(), text)
                    Result.success(parsed.content.joinToString("") { it.text.orEmpty() })
                } else {
                    val parsed = json.decodeFromString(OaChatResponse.serializer(), text)
                    Result.success(parsed.choices.firstOrNull()?.message?.content.orEmpty())
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Attempts automatic model discovery via the provider's models-list endpoint. */
    suspend fun listModels(provider: AiProvider): Result<List<AiModel>> {
        return try {
            val request = authorizedRequestBuilder(provider, provider.modelsListUrl).get().build()
            okHttpClient.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return Result.failure(IOException("HTTP ${resp.code}: $text"))
                val parsed = json.decodeFromString(OaModelsListResponse.serializer(), text)
                val now = System.currentTimeMillis()
                Result.success(
                    parsed.data.map {
                        AiModel(
                            id = "${provider.id}_${it.id}",
                            providerId = provider.id,
                            modelId = it.id,
                            displayName = it.id,
                            contextLength = it.contextLength ?: 8192,
                            source = ModelSource.DISCOVERED,
                            addedAt = now,
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
