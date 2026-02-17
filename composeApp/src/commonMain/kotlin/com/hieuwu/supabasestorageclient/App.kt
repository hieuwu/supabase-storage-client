package com.hieuwu.supabasestorageclient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hieuwu.supabasestorageclient.presentation.credentials.CredentialsScreen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingScreen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme {
        val onboardingViewModel: OnboardingViewModel = koinViewModel()
        val isOnboardingCompleted by onboardingViewModel.isCompleted.collectAsStateWithLifecycle()
        
        if (isOnboardingCompleted) {
            CredentialsScreen()
        } else {
            OnboardingScreen(
                onComplete = {
                    onboardingViewModel.completeOnboarding()
                }
            )
        }
    }
}