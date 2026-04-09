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
    onPurchaseStarted: (() -> Unit)? = null,
    onPurchaseCompleted: (CustomerInfo) -> Unit,
    onPurchaseError: (PurchasesError) -> Unit,
    onPurchaseCancelled: (() -> Unit)? = null,
    onRestoreStarted: (() -> Unit)? = null,
    onRestoreCompleted: (CustomerInfo) -> Unit,
    onRestoreError: (PurchasesError) -> Unit,
) {
    val options = PaywallOptions(dismissRequest = onDismiss) {
        this.offering = offering
        this.shouldDisplayDismissButton = true
        this.listener = object : PaywallListener {
            override fun onPurchaseStarted(rcPackage: com.revenuecat.purchases.kmp.models.Package) {
                super.onPurchaseStarted(rcPackage)
                onPurchaseStarted?.invoke()
            }

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

            override fun onPurchaseCancelled() {
                super.onPurchaseCancelled()
                onPurchaseCancelled?.invoke()
            }

            override fun onRestoreStarted() {
                super.onRestoreStarted()
                onRestoreStarted?.invoke()
            }

            override fun onRestoreError(error: PurchasesError) {
                onRestoreError(error)
            }

            override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                super.onRestoreCompleted(customerInfo)
                onRestoreCompleted(customerInfo)
            }
        }
    }
    Paywall(options = options)
}
