package com.vibecode.ide.data.repository

import com.vibecode.ide.data.local.dao.ProviderDao
import com.vibecode.ide.data.local.entity.ProviderEntity
import com.vibecode.ide.data.security.SecureKeyStore
import com.vibecode.ide.domain.model.AiProvider
import com.vibecode.ide.domain.model.AuthMethod
import com.vibecode.ide.domain.model.RequestFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val providerDao: ProviderDao,
    private val keyStore: SecureKeyStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeProviders(): Flow<List<AiProvider>> =
        providerDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getProvider(id: String): AiProvider? = providerDao.getById(id)?.toDomain()

    /** Creates a new provider. If apiKey is non-blank it is stored encrypted and referenced by alias. */
    suspend fun createProvider(
        name: String,
        baseUrl: String,
        chatCompletionsPath: String,
        modelsListPath: String,
        authMethod: AuthMethod,
        authHeaderName: String,
        requestFormat: RequestFormat,
        apiKey: String?,
        extraHeaders: Map<String, String> = emptyMap(),
    ): AiProvider {
        val alias = if (!apiKey.isNullOrBlank()) keyStore.storeNewSecret(apiKey) else ""
        val entity = ProviderEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            baseUrl = baseUrl,
            chatCompletionsPath = chatCompletionsPath,
            modelsListPath = modelsListPath,
            authMethod = authMethod.name,
            authHeaderName = authHeaderName,
            requestFormat = requestFormat.name,
            keyAlias = alias,
            isEnabled = true,
            createdAt = System.currentTimeMillis(),
            extraHeadersJson = if (extraHeaders.isNotEmpty()) json.encodeToString(extraHeaders) else null,
        )
        providerDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun updateProvider(
        id: String,
        name: String,
        baseUrl: String,
        chatCompletionsPath: String,
        modelsListPath: String,
        authMethod: AuthMethod,
        authHeaderName: String,
        requestFormat: RequestFormat,
        newApiKey: String?, // null = leave unchanged, blank = clear, non-blank = update
        isEnabled: Boolean,
        extraHeaders: Map<String, String> = emptyMap(),
    ) {
        val existing = providerDao.getById(id) ?: return
        var alias = existing.keyAlias
        if (newApiKey != null) {
            if (newApiKey.isBlank()) {
                if (alias.isNotBlank()) keyStore.deleteSecret(alias)
                alias = ""
            } else if (alias.isBlank()) {
                alias = keyStore.storeNewSecret(newApiKey)
            } else {
                keyStore.updateSecret(alias, newApiKey)
            }
        }
        providerDao.upsert(
            existing.copy(
                name = name, baseUrl = baseUrl, chatCompletionsPath = chatCompletionsPath,
                modelsListPath = modelsListPath, authMethod = authMethod.name,
                authHeaderName = authHeaderName, requestFormat = requestFormat.name,
                keyAlias = alias, isEnabled = isEnabled,
                extraHeadersJson = if (extraHeaders.isNotEmpty()) json.encodeToString(extraHeaders) else null,
            )
        )
    }

    suspend fun deleteProvider(provider: AiProvider) {
        val entity = providerDao.getById(provider.id) ?: return
        if (entity.keyAlias.isNotBlank()) keyStore.deleteSecret(entity.keyAlias)
        providerDao.delete(entity)
    }

    private fun ProviderEntity.toDomain(): AiProvider = AiProvider(
        id = id,
        name = name,
        baseUrl = baseUrl,
        chatCompletionsPath = chatCompletionsPath,
        modelsListPath = modelsListPath,
        authMethod = runCatching { AuthMethod.valueOf(authMethod) }.getOrDefault(AuthMethod.BEARER),
        authHeaderName = authHeaderName,
        requestFormat = runCatching { RequestFormat.valueOf(requestFormat) }.getOrDefault(RequestFormat.OPENAI),
        apiKey = keyStore.getSecret(keyAlias),
        isEnabled = isEnabled,
        createdAt = createdAt,
        extraHeaders = extraHeadersJson?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrDefault(emptyMap()) } ?: emptyMap(),
    )
}
