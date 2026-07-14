package com.vibecode.ide.domain.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Extracts a single ```tool ... ``` fenced JSON block from an assistant
 * message, if present. This is the prompt-based tool-calling protocol used so
 * that ANY provider (not just ones with native function-calling) can drive
 * the coding agent tools.
 */
object ToolCallParser {
    private val fenceRegex = Regex("```tool\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
    private val json = Json { ignoreUnknownKeys = true }

    fun extract(assistantText: String): ParsedToolCall? {
        val match = fenceRegex.find(assistantText) ?: return null
        val rawJson = match.groupValues[1].trim()
        return try {
            val obj = json.parseToJsonElement(rawJson) as? JsonObject ?: return null
            val toolId = obj["tool"]?.jsonPrimitive?.contentOrNull ?: return null
            val tool = ToolName.fromId(toolId) ?: return null
            ParsedToolCall(
                tool = tool,
                path = obj["path"]?.jsonPrimitive?.contentOrNull,
                content = obj["content"]?.jsonPrimitive?.contentOrNull,
                query = obj["query"]?.jsonPrimitive?.contentOrNull,
                rawArgsJson = rawJson,
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Strips the tool-call fenced block out of the display text, leaving the natural-language part. */
    fun stripToolBlock(assistantText: String): String =
        fenceRegex.replace(assistantText, "").trim()

    private val danglingFenceRegex = Regex("```tool\\s*\\n[\\s\\S]*$")

    /** Strips a still-open (never closed) ```tool fence — for a reply that got cut off mid tool-call. */
    fun stripDanglingToolBlock(assistantText: String): String =
        danglingFenceRegex.replace(assistantText, "").trim()
}
