package com.hieuwu.supabasestorageclient

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.hieuwu.supabasestorageclient.presentation.navigation.NavGraph
import com.hieuwu.supabasestorageclient.presentation.navigation.Screen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

import com.hieuwu.supabasestorageclient.presentation.theme.SupaBucktTheme

@Composable
fun App() {
    SupaBucktTheme {
        val onboardingViewModel: OnboardingViewModel = koinViewModel()
        val isOnboardingCompleted by onboardingViewModel.isCompleted.collectAsStateWithLifecycle()
        
        val supabaseClientManager: SupabaseClientManager = koinInject()
        val supabaseClient by supabaseClientManager.client.collectAsStateWithLifecycle()

        val navController = rememberNavController()

        val startDestination = remember {
            if (!isOnboardingCompleted) Screen.Onboarding.route
            else if (supabaseClient != null) Screen.Main.route
            else Screen.Credentials.route
        }

        LaunchedEffect(isOnboardingCompleted) {
            if (isOnboardingCompleted && navController.currentDestination?.route == Screen.Onboarding.route) {
                navController.navigate(Screen.Credentials.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }
        }

        LaunchedEffect(supabaseClient) {
            if (supabaseClient != null && navController.currentDestination?.route == Screen.Credentials.route) {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Credentials.route) { inclusive = true }
                }
            } else if (supabaseClient == null && navController.currentDestination?.route == Screen.Main.route) {
                navController.navigate(Screen.Credentials.route) {
                    popUpTo(Screen.Main.route) { inclusive = true }
                }
            }
        }

        NavGraph(
            navController = navController,
            startDestination = startDestination,
            onOnboardingComplete = {
                onboardingViewModel.completeOnboarding()
            },
            onLogout = {
                supabaseClientManager.clearClient()
            }
        )
    }
}