package com.vibecode.ide.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecode.ide.data.repository.AppSettings
import com.vibecode.ide.data.repository.BackupRepository
import com.vibecode.ide.data.repository.ModelRepository
import com.vibecode.ide.data.repository.ProviderRepository
import com.vibecode.ide.data.repository.SettingsRepository
import com.vibecode.ide.domain.model.AuthMethod
import com.vibecode.ide.domain.model.RequestFormat
import com.vibecode.ide.ui.theme.AppThemeMode
import com.vibecode.ide.ui.theme.EditorColorTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupUiState(val lastExportPath: String? = null, val message: String? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val providerRepository: ProviderRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState

    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setEditorTheme(theme: EditorColorTheme) = viewModelScope.launch { settingsRepository.setEditorTheme(theme) }
    fun setFontSize(size: Int) = viewModelScope.launch { settingsRepository.setFontSize(size) }
    fun setShowLineNumbers(show: Boolean) = viewModelScope.launch { settingsRepository.setShowLineNumbers(show) }
    fun setWordWrap(wrap: Boolean) = viewModelScope.launch { settingsRepository.setWordWrap(wrap) }
    fun setAutoSave(auto: Boolean) = viewModelScope.launch { settingsRepository.setAutoSave(auto) }
    fun setTabSize(size: Int) = viewModelScope.launch { settingsRepository.setTabSize(size) }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                val file = backupRepository.exportToFile()
                _backupState.value = BackupUiState(lastExportPath = file.absolutePath, message = "Exported to ${file.absolutePath}")
            } catch (e: Exception) {
                _backupState.value = BackupUiState(message = "Export failed: ${e.message}")
            }
        }
    }

    fun importBackup() {
        viewModelScope.launch {
            try {
                val file = File(backupRepository.backupDirectory(), "vibecode_backup.json")
                if (!file.exists()) {
                    _backupState.value = BackupUiState(message = "No backup file found at ${file.absolutePath}")
                    return@launch
                }
                val result = backupRepository.importFromFile(file)
                result.onSuccess { bundle ->
                    // Recreate providers (without keys — those must be re-entered for security)
                    // and their models.
                    val nameToNewId = mutableMapOf<String, String>()
                    bundle.providers.forEach { p ->
                        val created = providerRepository.createProvider(
                            name = p.name, baseUrl = p.baseUrl,
                            chatCompletionsPath = p.chatCompletionsPath, modelsListPath = p.modelsListPath,
                            authMethod = runCatching { AuthMethod.valueOf(p.authMethod) }.getOrDefault(AuthMethod.BEARER),
                            authHeaderName = p.authHeaderName,
                            requestFormat = runCatching { RequestFormat.valueOf(p.requestFormat) }.getOrDefault(RequestFormat.OPENAI),
                            apiKey = null,
                        )
                        nameToNewId[p.name] = created.id
                    }
                    bundle.models.forEach { m ->
                        val providerId = nameToNewId[m.providerName] ?: return@forEach
                        modelRepository.addManualModel(
                            providerId = providerId, modelId = m.modelId, displayName = m.displayName,
                            contextLength = m.contextLength, supportsStreaming = m.supportsStreaming,
                            supportsTools = m.supportsTools, supportsVision = m.supportsVision,
                        )
                    }
                    bundle.settings?.let { s ->
                        runCatching { AppThemeMode.valueOf(s.themeMode) }.getOrNull()?.let { settingsRepository.setThemeMode(it) }
                        runCatching { EditorColorTheme.valueOf(s.editorColorTheme) }.getOrNull()?.let { settingsRepository.setEditorTheme(it) }
                        settingsRepository.setFontSize(s.editorFontSizeSp)
                        settingsRepository.setShowLineNumbers(s.showLineNumbers)
                        settingsRepository.setWordWrap(s.wordWrap)
                        settingsRepository.setAutoSave(s.autoSave)
                        settingsRepository.setTabSize(s.tabSize)
                    }
                    _backupState.value = BackupUiState(message = "Restored ${bundle.providers.size} provider(s) and ${bundle.models.size} model(s). Re-enter API keys under Providers.")
                }.onFailure { e ->
                    _backupState.value = BackupUiState(message = "Import failed: ${e.message}")
                }
            } catch (e: Exception) {
                _backupState.value = BackupUiState(message = "Import failed: ${e.message}")
            }
        }
    }

    fun clearMessage() { _backupState.value = _backupState.value.copy(message = null) }
}
