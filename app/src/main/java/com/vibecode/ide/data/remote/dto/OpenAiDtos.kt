package com.vibecode.ide.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OaChatMessage(
    val role: String,
    val content: String? = null,
)

@Serializable
data class OaChatRequest(
    val model: String,
    val messages: List<OaChatMessage>,
    val stream: Boolean = true,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
data class OaChoiceDelta(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
data class OaStreamChoice(
    val index: Int = 0,
    val delta: OaChoiceDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class OaStreamChunk(
    val id: String? = null,
    val choices: List<OaStreamChoice> = emptyList(),
)

@Serializable
data class OaMessageFull(
    val role: String,
    val content: String? = null,
)

@Serializable
data class OaChoiceFull(
    val index: Int = 0,
    val message: OaMessageFull? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class OaChatResponse(
    val id: String? = null,
    val choices: List<OaChoiceFull> = emptyList(),
)

@Serializable
data class OaModelInfo(
    val id: String,
    @SerialName("context_length") val contextLength: Int? = null,
    @SerialName("owned_by") val ownedBy: String? = null,
)

@Serializable
data class OaModelsListResponse(
    val data: List<OaModelInfo> = emptyList(),
)

// --- Anthropic-format DTOs (used when RequestFormat.ANTHROPIC is selected) ---

@Serializable
data class AnthropicMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val stream: Boolean = true,
    val system: String? = null,
)

@Serializable
data class AnthropicContentBlock(
    val type: String,
    val text: String? = null,
)

@Serializable
data class AnthropicResponse(
    val id: String? = null,
    val content: List<AnthropicContentBlock> = emptyList(),
)

@Serializable
data class AnthropicStreamEvent(
    val type: String,
    val delta: AnthropicDelta? = null,
)

@Serializable
data class AnthropicDelta(
    val type: String? = null,
    val text: String? = null,
    // Present on "message_delta" events, e.g. "max_tokens" when the reply got cut off.
    @SerialName("stop_reason") val stopReason: String? = null,
)
