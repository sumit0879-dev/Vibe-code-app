package com.vibecode.ide.domain.model

import kotlinx.serialization.Serializable

enum class MessageRole { SYSTEM, USER, ASSISTANT, TOOL }

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCalls: List<ToolCallRecord> = emptyList(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
)

@Serializable
data class ToolCallRecord(
    val toolName: String,
    val argsJson: String,
    val resultSummary: String? = null,
    val approved: Boolean? = null, // null = pending, true = approved & applied, false = rejected
    val path: String? = null,
    val oldContent: String? = null, // file content before this change (null = file didn't exist, or non-mutating tool)
    val newContent: String? = null, // file content after this change (null for delete, or non-mutating tool)
    val revertedAt: Long? = null,   // set once the user reverts this specific change
)

data class ChatSession(
    val id: String,
    val projectId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String?,
    val modelId: String?,
)
