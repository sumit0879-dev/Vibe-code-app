package com.vibecode.ide.data.repository

import android.content.Context
import com.vibecode.ide.data.local.dao.ModelDao
import com.vibecode.ide.data.local.dao.ProviderDao
import com.vibecode.ide.data.local.entity.ModelEntity
import com.vibecode.ide.data.local.entity.ProviderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ProviderBackup(
    val name: String,
    val baseUrl: String,
    val chatCompletionsPath: String,
    val modelsListPath: String,
    val authMethod: String,
    val authHeaderName: String,
    val requestFormat: String,
    // API keys are intentionally NEVER included in exports.
)

@Serializable
data class ModelBackup(
    val providerName: String,
    val modelId: String,
    val displayName: String,
    val contextLength: Int,
    val supportsStreaming: Boolean,
    val supportsTools: Boolean,
    val supportsVision: Boolean,
)

@Serializable
data class SettingsBackup(
    val themeMode: String,
    val editorColorTheme: String,
    val editorFontSizeSp: Int,
    val showLineNumbers: Boolean,
    val wordWrap: Boolean,
    val autoSave: Boolean,
    val tabSize: Int,
)

@Serializable
data class BackupBundle(
    val version: Int = 1,
    val exportedAt: Long,
    val providers: List<ProviderBackup>,
    val models: List<ModelBackup>,
    val settings: SettingsBackup?,
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerDao: ProviderDao,
    private val modelDao: ModelDao,
    private val settingsRepository: SettingsRepository,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun backupDirectory(): File =
        File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    suspend fun exportToFile(fileName: String = "vibecode_backup.json"): File {
        val providerList = providerDao.observeAll().first()
        val nameById = providerList.associateBy({ it.id }, { it.name })
        val allModels = modelDao.observeAll().first()
        val modelBackups = allModels.mapNotNull { m: ModelEntity ->
            val providerName = nameById[m.providerId] ?: return@mapNotNull null
            ModelBackup(providerName, m.modelId, m.displayName, m.contextLength, m.supportsStreaming, m.supportsTools, m.supportsVision)
        }
        val s = settingsRepository.settings.first()
        val settingsBackup = SettingsBackup(
            s.themeMode.name, s.editorColorTheme.name, s.editorFontSizeSp,
            s.showLineNumbers, s.wordWrap, s.autoSave, s.tabSize,
        )

        val bundle = BackupBundle(
            exportedAt = System.currentTimeMillis(),
            providers = providerList.map {
                ProviderBackup(it.name, it.baseUrl, it.chatCompletionsPath, it.modelsListPath, it.authMethod, it.authHeaderName, it.requestFormat)
            },
            models = modelBackups,
            settings = settingsBackup,
        )
        val file = File(backupDirectory(), fileName)
        file.writeText(json.encodeToString(BackupBundle.serializer(), bundle))
        return file
    }

    suspend fun importFromFile(file: File): Result<BackupBundle> = try {
        val bundle = json.decodeFromString(BackupBundle.serializer(), file.readText())
        Result.success(bundle)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
