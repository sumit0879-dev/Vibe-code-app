package com.vibecode.ide.data.local.dao

import androidx.room.*
import com.vibecode.ide.data.local.entity.ChatMessageEntity
import com.vibecode.ide.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeSessions(projectId: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSession(id: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE projectId = :projectId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestSession(projectId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touchSession(id: String, timestamp: Long)

    @Query("UPDATE chat_sessions SET providerId = :providerId, modelId = :modelId WHERE id = :id")
    suspend fun updateSessionModel(id: String, providerId: String?, modelId: String?)

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :id")
    suspend fun renameSession(id: String, title: String)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessages(sessionId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearMessages(sessionId: String)

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)
}
