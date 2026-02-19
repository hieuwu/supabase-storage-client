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

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Credentials : Screen("credentials")
    object Main : Screen("main")
    object Search : Screen("search")
    object About : Screen("about")
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
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
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
