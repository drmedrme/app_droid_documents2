package com.documents.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.documents.app.ui.detail.DocumentDetailScreen
import com.documents.app.ui.home.HomeScreen
import com.documents.app.ui.search.SearchScreen
import com.documents.app.ui.settings.SettingsScreen
import com.documents.app.ui.upload.UploadScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    )

    data object Search : Screen(
        route = "search",
        title = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )

    data object Upload : Screen(
        route = "upload",
        title = "Upload",
        selectedIcon = Icons.Filled.CloudUpload,
        unselectedIcon = Icons.Outlined.CloudUpload
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object DocumentDetail : Screen(
        route = "document/{id}",
        title = "Detail",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Upload,
    Screen.Settings
)

@Composable
fun DocumentsNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onDocumentClick = { id ->
                    navController.navigate("document/$id")
                },
                viewModel = hiltViewModel()
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onDocumentClick = { id ->
                    navController.navigate("document/$id")
                },
                viewModel = hiltViewModel()
            )
        }

        composable(Screen.Upload.route) {
            UploadScreen(
                onUploadComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                viewModel = hiltViewModel()
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = hiltViewModel())
        }

        composable(
            route = Screen.DocumentDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("id") ?: return@composable
            DocumentDetailScreen(
                documentId = documentId,
                onNavigateBack = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
    }
}
