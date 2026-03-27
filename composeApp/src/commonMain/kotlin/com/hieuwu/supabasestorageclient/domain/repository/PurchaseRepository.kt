package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import com.revenuecat.purchases.kmp.models.Offering
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PurchaseRepository {
    /**
     * Emits true when the paywall should be displayed.
     */
    val showPaywallEvent: SharedFlow<Unit>

    val shouldEnablePurchase: Boolean

    /**
     * Current entitlement status.
     */
    val isPro: StateFlow<Boolean>

    /**
     * Trigger the paywall event.
     */
    fun triggerPaywall()

    /**
     * Initialize RevenueCat SDK.
     */
    fun initialize()
    
    /**
     * Check current entitlement status from RevenueCat.
     */
    suspend fun checkEntitlements()

    /**
     * Update the internal entitlement status using provided CustomerInfo.
     */
    fun updatePurchaseStatus(customerInfo: com.revenuecat.purchases.kmp.models.CustomerInfo)

    /**
     * Fetch the current offering from RevenueCat.
     */
    fun fetchOffering(): Flow<ApiResponse<Offering>>
}
