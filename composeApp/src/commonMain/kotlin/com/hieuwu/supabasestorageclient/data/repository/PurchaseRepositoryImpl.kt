package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.models.EntitlementInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PurchaseRepositoryImpl : PurchaseRepository {
    private val _showPaywallEvent = MutableSharedFlow<Unit>()
    override val showPaywallEvent: SharedFlow<Unit> = _showPaywallEvent.asSharedFlow()

    private val _isPro = MutableStateFlow(false)
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun triggerPaywall() {
        scope.launch {
            _showPaywallEvent.emit(Unit)
        }
    }

    override fun initialize() {
        // Placeholder API keys - user needs to replace these
        val apiKey = "" // Set your API key here or via platform specific config
        
        // Note: For KMP, you might want to pass these from platform modules
        // but for now we initialize with a placeholder if not empty
        if (apiKey.isNotEmpty()) {
            Purchases.configure(PurchasesConfiguration(apiKey))
            scope.launch {
                checkEntitlements()
            }
        }
    }

    override suspend fun checkEntitlements() {
        try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            val proEntitlement = customerInfo.entitlements["pro"]
            _isPro.value = proEntitlement?.isActive == true
        } catch (e: Exception) {
            // Handle error or log
            _isPro.value = false
        }
    }
}
