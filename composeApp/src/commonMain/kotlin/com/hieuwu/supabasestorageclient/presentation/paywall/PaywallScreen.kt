package com.hieuwu.supabasestorageclient.presentation.paywall

import androidx.compose.runtime.Composable

@Composable
expect fun PaywallScreen(
    offering: Any?,
    onDismiss: () -> Unit
)
