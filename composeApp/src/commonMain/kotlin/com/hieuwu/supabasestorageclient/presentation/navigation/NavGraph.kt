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
    object BucketsTab : Screen("buckets-tab")
    object StarredTab : Screen("starred-tab")
    object DownloadsTab : Screen("downloads-tab")
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
            SearchScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

    }
}
