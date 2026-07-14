package com.vibecode.ide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val modelId: String,          // the id sent to the API, e.g. "gpt-4o-mini"
    val displayName: String,
    val contextLength: Int = 8192,
    val supportsStreaming: Boolean = true,
    val supportsTools: Boolean = false,
    val supportsVision: Boolean = false,
    val isFavorite: Boolean = false,
    val source: String = "MANUAL", // MANUAL | DISCOVERED
    val addedAt: Long,
)
