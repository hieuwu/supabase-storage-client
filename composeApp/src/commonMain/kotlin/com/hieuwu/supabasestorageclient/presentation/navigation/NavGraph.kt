package com.hieuwu.supabasestorageclient.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.hieuwu.supabasestorageclient.presentation.about.AboutScreen
import com.hieuwu.supabasestorageclient.presentation.credentials.CredentialsScreen
import com.hieuwu.supabasestorageclient.presentation.filebrowser.BucketScreen
import com.hieuwu.supabasestorageclient.presentation.fileview.FileViewScreen
import com.hieuwu.supabasestorageclient.presentation.main.MainScreen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingScreen
import com.hieuwu.supabasestorageclient.presentation.search.SearchScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Credentials : Screen("credentials")
    object Main : Screen("main")
    object Search : Screen("search")
    object About : Screen("about")
    object BucketsTab : Screen("buckets-tab")
    object StarredTab : Screen("starred-tab")
    object DownloadsTab : Screen("downloads-tab")
    object UploadsTab : Screen("uploads-tab")
    object SettingsTab : Screen("settings-tab")
    object Bucket : Screen("bucket/{bucketId}?path={path}") {
        fun createRoute(bucketId: String, path: String? = null) = 
            "bucket/$bucketId" + if (path != null) "?path=$path" else ""
    }
    object FileView : Screen("file-view/{bucketId}/{fileName}?path={path}") {
        fun createRoute(bucketId: String, fileName: String, path: String? = null) = 
            "file-view/$bucketId/$fileName" + if (path != null) "?path=$path" else ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onComplete = onOnboardingComplete)
        }
        composable(Screen.Credentials.route) {
            CredentialsScreen()
        }
        composable(Screen.Main.route) {
            MainScreen(
                onLogout = onLogout,
                rootNavController = navController
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onNavigateToBucket = { bucketId ->
                    navController.navigate(Screen.Bucket.createRoute(bucketId))
                },
                onNavigateToFolder = { bucketId, path ->
                    navController.navigate(Screen.Bucket.createRoute(bucketId, path))
                },
                onNavigateToFile = { bucketId, fileName, path ->
                    navController.navigate(Screen.FileView.createRoute(bucketId, fileName, path))
                }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Bucket.route,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("path") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getString("bucketId") ?: ""
            val path = backStackEntry.arguments?.getString("path")
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Browse") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    BucketScreen(
                        bucketId = bucketId,
                        path = path,
                        onBack = { navController.popBackStack() },
                        onNavigateToFolder = { newPath ->
                            navController.navigate(Screen.Bucket.createRoute(bucketId, newPath))
                        },
                        onNavigateToFile = { bId, fileName, p ->
                            navController.navigate(Screen.FileView.createRoute(bId, fileName, p))
                        }
                    )
                }
            }
        }

        composable(
            route = Screen.FileView.route,
            arguments = listOf(
                navArgument("bucketId") { type = NavType.StringType },
                navArgument("fileName") { type = NavType.StringType },
                navArgument("path") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getString("bucketId") ?: ""
            val fileName = backStackEntry.arguments?.getString("fileName") ?: ""
            val path = backStackEntry.arguments?.getString("path")
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("File") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    FileViewScreen(
                        bucketId = bucketId,
                        fileName = fileName,
                        path = path,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

    }
}
