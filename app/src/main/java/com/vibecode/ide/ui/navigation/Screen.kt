package com.vibecode.ide.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Editor : Screen("editor/{projectId}") {
        fun createRoute(projectId: String) = "editor/$projectId"
    }
    data object Providers : Screen("providers")
    data object Models : Screen("models/{providerId}") {
        fun createRoute(providerId: String) = "models/$providerId"
    }
    data object AllModels : Screen("models_all")
    data object Settings : Screen("settings")
}
