package com.hieuwu.supabasestorageclient.presentation.paywall

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

@Composable
fun PaywallScreen(
    offering: Offering?,
    onDismiss: () -> Unit,
    onPurchaseCompleted: (CustomerInfo) -> Unit,
    onPurchaseError: (PurchasesError) -> Unit,
) {
    val options = PaywallOptions(dismissRequest = onDismiss) {
        this.offering = offering
        this.shouldDisplayDismissButton = true
        this.listener = object : PaywallListener {
            override fun onPurchaseCompleted(
                customerInfo: CustomerInfo,
                storeTransaction: StoreTransaction
            ) {
                super.onPurchaseCompleted(customerInfo, storeTransaction)
                onPurchaseCompleted(customerInfo)

            }

            override fun onPurchaseError(error: PurchasesError) {
                onPurchaseError(error)
            }
        }
    }
    Paywall(options = options)
}
