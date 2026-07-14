package com.vibecode.ide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,          // "user" | "assistant" | "system" | "tool"
    val content: String,
    val timestamp: Long,
    val toolCallsJson: String? = null,   // serialized list of tool calls made in this message
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
)
