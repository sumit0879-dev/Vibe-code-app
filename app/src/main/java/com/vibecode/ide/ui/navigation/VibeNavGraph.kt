package com.vibecode.ide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vibecode.ide.ui.editor.EditorScreen
import com.vibecode.ide.ui.home.HomeScreen
import com.vibecode.ide.ui.models.ModelManagerScreen
import com.vibecode.ide.ui.providers.ProviderManagerScreen
import com.vibecode.ide.ui.settings.SettingsScreen

@Composable
fun VibeNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenProject = { projectId -> navController.navigate(Screen.Editor.createRoute(projectId)) },
                onOpenProviders = { navController.navigate(Screen.Providers.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
            EditorScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() },
                onOpenProviders = { navController.navigate(Screen.Providers.route) },
                onOpenModels = { providerId -> navController.navigate(Screen.Models.createRoute(providerId)) },
            )
        }
        composable(Screen.Providers.route) {
            ProviderManagerScreen(
                onBack = { navController.popBackStack() },
                onOpenModels = { providerId -> navController.navigate(Screen.Models.createRoute(providerId)) },
            )
        }
        composable(
            route = Screen.Models.route,
            arguments = listOf(navArgument("providerId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId").orEmpty()
            ModelManagerScreen(providerId = providerId, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenProviders = { navController.navigate(Screen.Providers.route) },
            )
        }
    }
}
