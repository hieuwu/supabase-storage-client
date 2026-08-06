package com.hieuwu.supabasestorageclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.AppTheme
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.presentation.navigation.NavGraph
import com.hieuwu.supabasestorageclient.presentation.navigation.Screen
import com.hieuwu.supabasestorageclient.presentation.onboarding.OnboardingViewModel
import com.hieuwu.supabasestorageclient.presentation.paywall.PaywallScreen
import com.hieuwu.supabasestorageclient.presentation.settings.SettingsViewModel
import com.hieuwu.supabasestorageclient.presentation.settings.SettingsUiState
import com.hieuwu.supabasestorageclient.presentation.theme.SupaBucktTheme
import com.revenuecat.purchases.kmp.models.Offering
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val currentTheme = (settingsState as? SettingsUiState.Content)?.settings?.theme ?: AppTheme.SYSTEM
    
    SupaBucktTheme(appTheme = currentTheme) {
        val onboardingViewModel: OnboardingViewModel = koinViewModel()
        val isOnboardingCompleted by onboardingViewModel.isCompleted.collectAsStateWithLifecycle()
        
        val supabaseClientManager: SupabaseClientManager = koinInject()
        val supabaseClient by supabaseClientManager.client.collectAsStateWithLifecycle()

        val purchaseRepository: PurchaseRepository = koinInject()
        var showGlobalPaywall by remember { mutableStateOf(false) }
        var currentOffering by remember { mutableStateOf<Offering?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            purchaseRepository.initialize()
            purchaseRepository.showPaywallEvent.collect {
                purchaseRepository.fetchOffering().collect { response ->
                    when (response) {
                        is ApiResponse.Success -> {
                            println("RevenueCat: Successfully fetched offering: ${response.data.identifier}")
                            currentOffering = response.data
                            showGlobalPaywall = true
                        }
                        is ApiResponse.Error -> {
                            println("RevenueCat: Failed to fetch offering: ${response.exception?.message}")
                            showGlobalPaywall = true
                        }
                        is ApiResponse.Loading -> {
                            println("RevenueCat: Fetching offering...")
                        }
                    }
                }
            }
        }

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

        Box(modifier = Modifier.fillMaxSize()) {
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


            if (showGlobalPaywall) {
                PaywallScreen(
                    offering = currentOffering,
                    onDismiss = { showGlobalPaywall = false },
                    onPurchaseStarted = {
                        scope.launch { snackbarHostState.showSnackbar("Starting purchase...") }
                    },
                    onPurchaseCompleted = { customerInfo ->
                        purchaseRepository.updatePurchaseStatus(customerInfo)
                        scope.launch {
                            snackbarHostState.showSnackbar("Purchase successful!")
                        }
                        showGlobalPaywall = false
                    },
                    onPurchaseError = { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Purchase failed: ${error.message}")
                        }
                    },
                    onPurchaseCancelled = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Purchase cancelled")
                        }
                    },
                    onRestoreStarted = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Restoring purchases...")
                        }
                    },
                    onRestoreCompleted = { customerInfo ->
                        purchaseRepository.updatePurchaseStatus(customerInfo)
                        scope.launch {
                            snackbarHostState.showSnackbar("Restore successful!")
                        }
                        showGlobalPaywall = false
                    },
                    onRestoreError = { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Restore failed: ${error.message}")
                        }
                    }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}