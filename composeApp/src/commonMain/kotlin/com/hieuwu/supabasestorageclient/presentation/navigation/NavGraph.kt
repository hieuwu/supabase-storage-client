package com.hieuwu.supabasestorageclient.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hieuwu.supabasestorageclient.presentation.about.AboutScreen
import com.hieuwu.supabasestorageclient.presentation.credentials.CredentialsScreen
import com.hieuwu.supabasestorageclient.presentation.main.MainScreen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingScreen
import com.hieuwu.supabasestorageclient.presentation.search.SearchScreen

import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Credentials : Screen("credentials")
    object Main : Screen("main")
    object Search : Screen("search")
    object About : Screen("about")
    object Bucket : Screen("bucket/{bucketId}?path={path}") {
        fun createRoute(bucketId: String, path: String? = null) = 
            "bucket/$bucketId" + if (path != null) "?path=$path" else ""
    }
    object FileView : Screen("file-view/{bucketId}/{fileName}?path={path}") {
        fun createRoute(bucketId: String, fileName: String, path: String? = null) = 
            "file-view/$bucketId/$fileName" + if (path != null) "?path=$path" else ""
    }
}

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
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToBucket = { bucketId -> 
                    navController.navigate(Screen.Bucket.createRoute(bucketId))
                }
            )
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
            val bucketId = backStackEntry.arguments?.get("bucketId")?.toString().orEmpty()
            val path = backStackEntry.arguments?.get("path")?.toString()
            com.hieuwu.supabasestorageclient.presentation.bucket.BucketScreen(
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

        composable(Screen.Search.route) {
            SearchScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
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
            val bucketId = backStackEntry.arguments?.getString("bucketId").orEmpty()
            val fileName = backStackEntry.arguments?.getString("fileName").orEmpty()
            val path = backStackEntry.arguments?.getString("path")
            com.hieuwu.supabasestorageclient.presentation.fileview.FileViewScreen(
                bucketId = bucketId,
                fileName = fileName,
                path = path,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
