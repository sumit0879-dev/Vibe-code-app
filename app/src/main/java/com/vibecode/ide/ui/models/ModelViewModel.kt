package com.vibecode.ide.ui.models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecode.ide.data.repository.ModelRepository
import com.vibecode.ide.data.repository.ProviderRepository
import com.vibecode.ide.domain.model.AiModel
import com.vibecode.ide.domain.model.AiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelUiState(
    val provider: AiProvider? = null,
    val isDiscovering: Boolean = false,
    val discoveryError: String? = null,
)

@HiltViewModel
class ModelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val modelRepository: ModelRepository,
    private val providerRepository: ProviderRepository,
) : ViewModel() {

    private val providerId: String = checkNotNull(savedStateHandle["providerId"])

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState

    val models: StateFlow<List<AiModel>> = modelRepository.observeForProvider(providerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(provider = providerRepository.getProvider(providerId))
        }
    }

    fun discoverModels() {
        val provider = _uiState.value.provider ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDiscovering = true, discoveryError = null)
            val result = modelRepository.discoverModels(provider)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(discoveryError = e.message ?: "Discovery failed")
            }
            _uiState.value = _uiState.value.copy(isDiscovering = false)
        }
    }

    fun addManualModel(modelId: String, displayName: String, contextLength: Int, supportsTools: Boolean, supportsVision: Boolean) {
        viewModelScope.launch {
            modelRepository.addManualModel(
                providerId = providerId, modelId = modelId,
                displayName = displayName, contextLength = contextLength,
                supportsStreaming = true, supportsTools = supportsTools, supportsVision = supportsVision,
            )
        }
    }

    fun toggleFavorite(model: AiModel) = viewModelScope.launch { modelRepository.toggleFavorite(model) }
    fun deleteModel(model: AiModel) = viewModelScope.launch { modelRepository.deleteModel(model) }
    fun clearError() { _uiState.value = _uiState.value.copy(discoveryError = null) }
}
