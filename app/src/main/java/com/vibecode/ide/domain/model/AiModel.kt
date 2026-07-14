package com.vibecode.ide.domain.model

enum class ModelSource { MANUAL, DISCOVERED }

data class AiModel(
    val id: String,
    val providerId: String,
    val modelId: String,
    val displayName: String,
    val contextLength: Int = 8192,
    val supportsStreaming: Boolean = true,
    val supportsTools: Boolean = false,
    val supportsVision: Boolean = false,
    val isFavorite: Boolean = false,
    val source: ModelSource = ModelSource.MANUAL,
    val addedAt: Long = System.currentTimeMillis(),
)
