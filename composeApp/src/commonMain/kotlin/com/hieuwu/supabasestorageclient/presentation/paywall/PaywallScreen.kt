package com.hieuwu.supabasestorageclient.presentation.paywall

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

@Composable
fun PaywallScreen(
    offering: Offering?,
    onDismiss: () -> Unit
) {
    val options = PaywallOptions(dismissRequest = onDismiss) {
        this.offering = offering
        this.shouldDisplayDismissButton = true
    }
    Paywall(options = options)
}
