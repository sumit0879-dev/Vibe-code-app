package com.vibecode.ide.data.repository

import com.vibecode.ide.data.local.dao.ModelDao
import com.vibecode.ide.data.local.entity.ModelEntity
import com.vibecode.ide.data.remote.AiClient
import com.vibecode.ide.domain.model.AiModel
import com.vibecode.ide.domain.model.AiProvider
import com.vibecode.ide.domain.model.ModelSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    private val modelDao: ModelDao,
    private val aiClient: AiClient,
) {
    fun observeForProvider(providerId: String): Flow<List<AiModel>> =
        modelDao.observeForProvider(providerId).map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<AiModel>> =
        modelDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: String): AiModel? = modelDao.getById(id)?.toDomain()

    suspend fun addManualModel(
        providerId: String,
        modelId: String,
        displayName: String,
        contextLength: Int,
        supportsStreaming: Boolean,
        supportsTools: Boolean,
        supportsVision: Boolean,
    ): AiModel {
        val entity = ModelEntity(
            id = UUID.randomUUID().toString(),
            providerId = providerId,
            modelId = modelId,
            displayName = displayName.ifBlank { modelId },
            contextLength = contextLength,
            supportsStreaming = supportsStreaming,
            supportsTools = supportsTools,
            supportsVision = supportsVision,
            source = ModelSource.MANUAL.name,
            addedAt = System.currentTimeMillis(),
        )
        modelDao.upsert(entity)
        return entity.toDomain()
    }

    /** Calls the provider's model-listing endpoint and stores the results as DISCOVERED models. */
    suspend fun discoverModels(provider: AiProvider): Result<List<AiModel>> {
        val result = aiClient.listModels(provider)
        result.onSuccess { models ->
            modelDao.clearDiscovered(provider.id)
            modelDao.upsertAll(models.map { it.toEntity() })
        }
        return result
    }

    suspend fun toggleFavorite(model: AiModel) {
        modelDao.upsert(model.copy(isFavorite = !model.isFavorite).toEntity())
    }

    suspend fun deleteModel(model: AiModel) {
        modelDao.delete(model.toEntity())
    }

    private fun ModelEntity.toDomain() = AiModel(
        id = id, providerId = providerId, modelId = modelId, displayName = displayName,
        contextLength = contextLength, supportsStreaming = supportsStreaming,
        supportsTools = supportsTools, supportsVision = supportsVision,
        isFavorite = isFavorite,
        source = runCatching { ModelSource.valueOf(source) }.getOrDefault(ModelSource.MANUAL),
        addedAt = addedAt,
    )

    private fun AiModel.toEntity() = ModelEntity(
        id = id, providerId = providerId, modelId = modelId, displayName = displayName,
        contextLength = contextLength, supportsStreaming = supportsStreaming,
        supportsTools = supportsTools, supportsVision = supportsVision,
        isFavorite = isFavorite, source = source.name, addedAt = addedAt,
    )
}
