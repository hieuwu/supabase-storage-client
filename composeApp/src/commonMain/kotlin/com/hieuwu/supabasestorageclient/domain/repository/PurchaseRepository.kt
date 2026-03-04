package com.hieuwu.supabasestorageclient.domain.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface PurchaseRepository {
    /**
     * Emits true when the paywall should be displayed.
     */
    val showPaywallEvent: SharedFlow<Unit>

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
}
