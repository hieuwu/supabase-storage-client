package com.hieuwu.supabasestorageclient.presentation.onboarding

/**
 * UI state for the onboarding flow
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3,
    val securityWarningAcknowledged: Boolean = false
)
