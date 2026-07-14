package com.vibecode.ide.domain.model

/** How the API key is attached to outgoing requests. */
enum class AuthMethod { BEARER, API_KEY_HEADER, QUERY_PARAM, NONE }

/** Wire format the provider expects request/response bodies in. */
enum class RequestFormat { OPENAI, ANTHROPIC, CUSTOM }

/**
 * A user-configured AI backend. Completely generic — the app ships with none
 * pre-configured. Works with any OpenAI-compatible endpoint out of the box,
 * and can be adapted to others via requestFormat/authMethod/authHeaderName.
 */
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val chatCompletionsPath: String = "/chat/completions",
    val modelsListPath: String = "/models",
    val authMethod: AuthMethod = AuthMethod.BEARER,
    val authHeaderName: String = "Authorization",
    val requestFormat: RequestFormat = RequestFormat.OPENAI,
    val apiKey: String? = null,       // decrypted, in-memory only — never persisted as-is
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val extraHeaders: Map<String, String> = emptyMap(),
) {
    val chatCompletionsUrl: String get() = baseUrl.trimEnd('/') + chatCompletionsPath
    val modelsListUrl: String get() = baseUrl.trimEnd('/') + modelsListPath
}
