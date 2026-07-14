package com.vibecode.ide.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecode.ide.data.repository.ProviderRepository
import com.vibecode.ide.domain.model.AiProvider
import com.vibecode.ide.domain.model.AuthMethod
import com.vibecode.ide.domain.model.RequestFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderFormState(
    val id: String? = null, // null = creating new
    val name: String = "",
    val baseUrl: String = "",
    val chatCompletionsPath: String = "/chat/completions",
    val modelsListPath: String = "/models",
    val authMethod: AuthMethod = AuthMethod.BEARER,
    val authHeaderName: String = "Authorization",
    val requestFormat: RequestFormat = RequestFormat.OPENAI,
    val apiKey: String = "",
)

data class ProviderUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
) : ViewModel() {

    val providers: StateFlow<List<AiProvider>> = providerRepository.observeProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ProviderUiState())
    val uiState: StateFlow<ProviderUiState> = _uiState

    fun save(form: ProviderFormState, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            try {
                if (form.baseUrl.isBlank()) throw IllegalArgumentException("Base URL is required")
                if (form.id == null) {
                    providerRepository.createProvider(
                        name = form.name.ifBlank { form.baseUrl },
                        baseUrl = form.baseUrl,
                        chatCompletionsPath = form.chatCompletionsPath,
                        modelsListPath = form.modelsListPath,
                        authMethod = form.authMethod,
                        authHeaderName = form.authHeaderName,
                        requestFormat = form.requestFormat,
                        apiKey = form.apiKey.ifBlank { null },
                    )
                } else {
                    providerRepository.updateProvider(
                        id = form.id,
                        name = form.name.ifBlank { form.baseUrl },
                        baseUrl = form.baseUrl,
                        chatCompletionsPath = form.chatCompletionsPath,
                        modelsListPath = form.modelsListPath,
                        authMethod = form.authMethod,
                        authHeaderName = form.authHeaderName,
                        requestFormat = form.requestFormat,
                        newApiKey = form.apiKey.ifBlank { null },
                        isEnabled = true,
                    )
                }
                _uiState.value = _uiState.value.copy(isSaving = false)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = e.message)
            }
        }
    }

    fun delete(provider: AiProvider) {
        viewModelScope.launch { providerRepository.deleteProvider(provider) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
