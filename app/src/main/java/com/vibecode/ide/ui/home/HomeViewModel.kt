package com.vibecode.ide.ui.home

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecode.ide.data.repository.ProjectRepository
import com.vibecode.ide.domain.model.Project
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class HomeUiState(
    val recentProjects: List<Project> = emptyList(),
    val defaultProjectsRoot: String = "",
    val isCreatingProject: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
) : ViewModel() {

    private val defaultRoot = File(
        Environment.getExternalStorageDirectory(),
        "VibeCodeProjects",
    ).absolutePath

    private val _uiState = MutableStateFlow(HomeUiState(defaultProjectsRoot = defaultRoot))
    val uiState: StateFlow<HomeUiState> = _uiState

    val recentProjects: StateFlow<List<Project>> = projectRepository.observeRecents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProject(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingProject = true, errorMessage = null)
            try {
                val safeName = name.trim().ifBlank { "UntitledProject" }
                val path = File(defaultRoot, safeName).absolutePath
                // A new project starts as a completely empty folder — no scaffold or
                // template files are generated. The user (or the AI agent) decides
                // what goes in it.
                val project = projectRepository.openOrCreateProject(safeName, path)
                _uiState.value = _uiState.value.copy(isCreatingProject = false)
                onCreated(project.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isCreatingProject = false, errorMessage = e.message)
            }
        }
    }

    fun openProjectAtPath(path: String, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val dir = File(path)
                val project = projectRepository.openOrCreateProject(dir.name.ifBlank { "Project" }, dir.absolutePath)
                onOpened(project.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun deleteProject(project: Project, alsoDeleteFiles: Boolean) {
        viewModelScope.launch {
            projectRepository.deleteProject(project, alsoDeleteFiles)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
