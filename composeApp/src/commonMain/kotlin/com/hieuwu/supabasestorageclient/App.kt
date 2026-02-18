package com.hieuwu.supabasestorageclient

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hieuwu.supabasestorageclient.presentation.credentials.CredentialsScreen
import com.hieuwu.supabasestorageclient.presentation.main.MainScreen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingScreen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme {
        val onboardingViewModel: OnboardingViewModel = koinViewModel()
        val isOnboardingCompleted by onboardingViewModel.isCompleted.collectAsStateWithLifecycle()
        
        val supabaseClientManager: SupabaseClientManager = koinInject()
        val supabaseClient by supabaseClientManager.client.collectAsStateWithLifecycle()

        if (!isOnboardingCompleted) {
            OnboardingScreen(
                onComplete = {
                    onboardingViewModel.completeOnboarding()
                }
            )
        } else if (supabaseClient != null) {
            MainScreen(
                onBack = {
                    supabaseClientManager.clearClient()
                }
            )
        } else {
            CredentialsScreen()
        }
    }
}