package com.vibecode.ide.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibecode.ide.data.repository.FileRepository
import com.vibecode.ide.data.repository.ProjectRepository
import com.vibecode.ide.domain.model.FileNode
import com.vibecode.ide.domain.model.Project
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpenTab(
    val path: String,
    val name: String,
    val content: String,
    val originalContent: String,
    val isDirty: Boolean = false,
    val version: Int = 0,
) {
    companion object {
        fun clean(path: String, name: String, content: String, version: Int = 0) =
            OpenTab(path, name, content, content, false, version)
    }
}

data class EditorUiState(
    val project: Project? = null,
    val rootNode: FileNode? = null,
    // Expanded directory paths -> their loaded children, so the tree can be lazily browsed.
    val expandedChildren: Map<String, List<FileNode>> = emptyMap(),
    val openTabs: List<OpenTab> = emptyList(),
    val activeTabIndex: Int = -1,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    val projectId: String = checkNotNull(savedStateHandle["projectId"])

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

    init {
        viewModelScope.launch {
            val project = projectRepository.getProject(projectId)
            projectRepository.touch(projectId)
            try {
                val children = project?.let { fileRepository.listChildren(it.rootPath, it.rootPath) } ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    project = project,
                    rootNode = project?.let { FileNode(it.name, it.rootPath, true, children) },
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    project = project,
                    rootNode = project?.let { FileNode(it.name, it.rootPath, true, emptyList()) },
                    isLoading = false,
                    errorMessage = e.message,
                )
            }
        }
    }

    fun toggleDirectory(node: FileNode) {
        val project = _uiState.value.project ?: return
        val current = _uiState.value.expandedChildren
        if (current.containsKey(node.path)) {
            _uiState.value = _uiState.value.copy(expandedChildren = current - node.path)
        } else {
            viewModelScope.launch {
                try {
                    val children = fileRepository.listChildren(project.rootPath, node.path)
                    _uiState.value = _uiState.value.copy(expandedChildren = _uiState.value.expandedChildren + (node.path to children))
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
            }
        }
    }

    fun openFile(node: FileNode) {
        if (node.isDirectory) return
        val project = _uiState.value.project ?: return
        val existingIndex = _uiState.value.openTabs.indexOfFirst { it.path == node.path }
        if (existingIndex >= 0) {
            _uiState.value = _uiState.value.copy(activeTabIndex = existingIndex)
            return
        }
        viewModelScope.launch {
            val result = fileRepository.readFile(project.rootPath, node.path)
            result.onSuccess { content ->
                val tab = OpenTab.clean(node.path, node.name, content)
                val newTabs = _uiState.value.openTabs + tab
                _uiState.value = _uiState.value.copy(openTabs = newTabs, activeTabIndex = newTabs.lastIndex)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    /** Opens/refreshes a tab with new content — used after the AI applies an approved edit. */
    fun openOrRefreshFileWithContent(path: String, name: String, content: String) {
        val existingIndex = _uiState.value.openTabs.indexOfFirst { it.path == path }
        val newTabs = if (existingIndex >= 0) {
            val prevVersion = _uiState.value.openTabs[existingIndex].version
            _uiState.value.openTabs.toMutableList().apply { this[existingIndex] = OpenTab.clean(path, name, content, prevVersion + 1) }
        } else {
            _uiState.value.openTabs + OpenTab.clean(path, name, content)
        }
        val activeIndex = newTabs.indexOfFirst { it.path == path }
        _uiState.value = _uiState.value.copy(openTabs = newTabs, activeTabIndex = activeIndex)
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(activeTabIndex = index)
    }

    fun closeTab(index: Int) {
        val tabs = _uiState.value.openTabs.toMutableList()
        if (index !in tabs.indices) return
        tabs.removeAt(index)
        val newActive = when {
            tabs.isEmpty() -> -1
            index <= _uiState.value.activeTabIndex && _uiState.value.activeTabIndex > 0 -> _uiState.value.activeTabIndex - 1
            else -> minOf(_uiState.value.activeTabIndex, tabs.lastIndex)
        }
        _uiState.value = _uiState.value.copy(openTabs = tabs, activeTabIndex = newActive)
    }

    fun updateActiveTabContent(newContent: String) {
        val idx = _uiState.value.activeTabIndex
        if (idx !in _uiState.value.openTabs.indices) return
        val tabs = _uiState.value.openTabs.toMutableList()
        val tab = tabs[idx]
        tabs[idx] = tab.copy(content = newContent, isDirty = newContent != tab.originalContent)
        _uiState.value = _uiState.value.copy(openTabs = tabs)
    }

    fun saveActiveTab() {
        val project = _uiState.value.project ?: return
        val idx = _uiState.value.activeTabIndex
        if (idx !in _uiState.value.openTabs.indices) return
        val tab = _uiState.value.openTabs[idx]
        if (!tab.isDirty) return
        viewModelScope.launch {
            fileRepository.writeFile(project.rootPath, tab.path, tab.content).onSuccess {
                val tabs = _uiState.value.openTabs.toMutableList()
                tabs[idx] = tab.copy(originalContent = tab.content, isDirty = false)
                _uiState.value = _uiState.value.copy(openTabs = tabs)
                refreshTree()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun refreshTree() {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            try {
                val children = fileRepository.listChildren(project.rootPath, project.rootPath)
                _uiState.value = _uiState.value.copy(rootNode = FileNode(project.name, project.rootPath, true, children))
                // Re-fetch any expanded directories so the tree stays in sync after AI edits.
                val expanded = _uiState.value.expandedChildren.keys.toList()
                val refreshed = expanded.associateWith { fileRepository.listChildren(project.rootPath, it) }
                _uiState.value = _uiState.value.copy(expandedChildren = refreshed)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    suspend fun buildFullTree(): FileNode? {
        val project = _uiState.value.project ?: return null
        return fileRepository.buildTree(project.rootPath)
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
}
