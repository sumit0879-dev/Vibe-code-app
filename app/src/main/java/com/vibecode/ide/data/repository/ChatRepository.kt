package com.vibecode.ide.data.repository

import com.vibecode.ide.data.local.dao.ChatDao
import com.vibecode.ide.data.local.entity.ChatMessageEntity
import com.vibecode.ide.data.local.entity.ChatSessionEntity
import com.vibecode.ide.domain.model.ChatMessage
import com.vibecode.ide.domain.model.ChatSession
import com.vibecode.ide.domain.model.MessageRole
import com.vibecode.ide.domain.model.ToolCallRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeSessions(projectId: String): Flow<List<ChatSession>> =
        chatDao.observeSessions(projectId).map { list -> list.map { it.toDomain() } }

    fun observeMessages(sessionId: String): Flow<List<ChatMessage>> =
        chatDao.observeMessages(sessionId).map { list -> list.map { it.toDomain() } }

    /** Returns the most recent session for a project, or creates a fresh one — this is how
     *  "continue from previous chat" works when a project is reopened. */
    suspend fun getOrCreateLatestSession(projectId: String, providerId: String?, modelId: String?): ChatSession {
        chatDao.getLatestSession(projectId)?.let { return it.toDomain() }
        val now = System.currentTimeMillis()
        val entity = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            title = "New chat",
            createdAt = now,
            updatedAt = now,
            providerId = providerId,
            modelId = modelId,
        )
        chatDao.upsertSession(entity)
        return entity.toDomain()
    }

    suspend fun createSession(projectId: String, title: String, providerId: String?, modelId: String?): ChatSession {
        val now = System.currentTimeMillis()
        val entity = ChatSessionEntity(
            id = UUID.randomUUID().toString(), projectId = projectId, title = title,
            createdAt = now, updatedAt = now, providerId = providerId, modelId = modelId,
        )
        chatDao.upsertSession(entity)
        return entity.toDomain()
    }

    suspend fun setSessionModel(sessionId: String, providerId: String?, modelId: String?) =
        chatDao.updateSessionModel(sessionId, providerId, modelId)

    suspend fun renameSession(sessionId: String, title: String) = chatDao.renameSession(sessionId, title)

    suspend fun deleteSession(session: ChatSession) {
        chatDao.getSession(session.id)?.let { chatDao.deleteSession(it) }
    }

    suspend fun appendMessage(message: ChatMessage): ChatMessage {
        chatDao.upsertMessage(message.toEntity())
        chatDao.touchSession(message.sessionId, System.currentTimeMillis())
        return message
    }

    suspend fun updateMessage(message: ChatMessage) {
        chatDao.upsertMessage(message.toEntity())
    }

    suspend fun getMessages(sessionId: String): List<ChatMessage> =
        chatDao.getMessages(sessionId).map { it.toDomain() }

    fun newMessage(
        sessionId: String, role: MessageRole, content: String,
        isStreaming: Boolean = false, isError: Boolean = false,
        toolCalls: List<ToolCallRecord> = emptyList(),
    ) = ChatMessage(
        id = UUID.randomUUID().toString(), sessionId = sessionId, role = role, content = content,
        timestamp = System.currentTimeMillis(), toolCalls = toolCalls, isStreaming = isStreaming, isError = isError,
    )

    private fun ChatSessionEntity.toDomain() = ChatSession(
        id = id, projectId = projectId, title = title, createdAt = createdAt,
        updatedAt = updatedAt, providerId = providerId, modelId = modelId,
    )

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id, sessionId = sessionId,
        role = runCatching { MessageRole.valueOf(role.uppercase()) }.getOrDefault(MessageRole.USER),
        content = content, timestamp = timestamp,
        toolCalls = toolCallsJson?.let { runCatching { json.decodeFromString<List<ToolCallRecord>>(it) }.getOrDefault(emptyList()) } ?: emptyList(),
        isStreaming = isStreaming, isError = isError,
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id, sessionId = sessionId, role = role.name.lowercase(), content = content,
        timestamp = timestamp,
        toolCallsJson = if (toolCalls.isNotEmpty()) json.encodeToString(toolCalls) else null,
        isStreaming = isStreaming, isError = isError,
    )
}
