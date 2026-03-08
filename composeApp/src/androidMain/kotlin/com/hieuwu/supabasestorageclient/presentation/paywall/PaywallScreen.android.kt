package com.hieuwu.supabasestorageclient.presentation.paywall

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

@Composable
actual fun PaywallScreen(
    offering: Any?,
    onDismiss: () -> Unit
) {
    val currentOffering = offering as? Offering
    val options = PaywallOptions(dismissRequest = onDismiss) {
        this.offering = currentOffering
        this.shouldDisplayDismissButton = true
    }
    Paywall(options = options)
}
