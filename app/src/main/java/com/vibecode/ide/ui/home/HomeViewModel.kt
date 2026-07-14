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
                val project = projectRepository.openOrCreateProject(safeName, path)
                scaffoldAndroidProject(File(project.rootPath), safeName)
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

    /** Lays out a minimal but real Android project skeleton so the AI has something to work with immediately. */
    private fun scaffoldAndroidProject(root: File, name: String) {
        if (File(root, "settings.gradle.kts").exists()) return // already scaffolded
        val pkg = "com.example." + name.lowercase().filter { it.isLetterOrDigit() }.ifBlank { "app" }
        val pkgPath = pkg.replace('.', '/')

        File(root, "app/src/main/java/$pkgPath").mkdirs()
        File(root, "app/src/main/res/values").mkdirs()

        File(root, "settings.gradle.kts").writeText(
            "rootProject.name = \"$name\"\ninclude(\":app\")\n"
        )
        File(root, "app/build.gradle.kts").writeText(
            """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            }
            android {
                namespace = "$pkg"
                compileSdk = 34
                defaultConfig {
                    applicationId = "$pkg"
                    minSdk = 24
                    targetSdk = 34
                    versionCode = 1
                    versionName = "1.0"
                }
            }
            """.trimIndent()
        )
        File(root, "app/src/main/AndroidManifest.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application android:label="$name">
                    <activity android:name=".MainActivity" android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
            """.trimIndent()
        )
        File(root, "app/src/main/java/$pkgPath/MainActivity.kt").writeText(
            """
            package $pkg

            import android.app.Activity
            import android.os.Bundle

            class MainActivity : Activity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                }
            }
            """.trimIndent()
        )
        File(root, "README.md").writeText("# $name\n\nCreated with VibeCode AI IDE.\n")
    }
}
