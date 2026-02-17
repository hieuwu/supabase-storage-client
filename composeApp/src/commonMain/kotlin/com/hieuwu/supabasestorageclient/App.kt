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
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingScreen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MaterialTheme {
        val onboardingViewModel: OnboardingViewModel = koinViewModel()
        val isOnboardingCompleted by onboardingViewModel.isCompleted.collectAsStateWithLifecycle()
        
        if (isOnboardingCompleted) {
            // Main app content - placeholder for now
            MainContent()
        } else {
            OnboardingScreen(
                onComplete = {
                    // Navigate to add project screen or main content
                    // For now, onboarding completion will trigger recomposition
                }
            )
        }
    }
}

@Composable
private fun MainContent() {
    // Placeholder main content
    // TODO: Replace with actual project list or add project screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Supabase Storage Manager!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Main app content goes here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}