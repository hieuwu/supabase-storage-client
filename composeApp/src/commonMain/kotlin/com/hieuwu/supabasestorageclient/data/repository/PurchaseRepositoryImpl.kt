package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.BuildKonfig

import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesException
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.models.Offering
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.observability.analytics.AnalyticsUserProperties
import com.hieuwu.supabasestorageclient.observability.analytics.AppAnalytics
import com.revenuecat.purchases.kmp.ktx.awaitRestore

class PurchaseRepositoryImpl(
    private val logger: Logger
) : PurchaseRepository {
    private val _showPaywallEvent = MutableSharedFlow<Unit>()
    override val showPaywallEvent: SharedFlow<Unit> = _showPaywallEvent.asSharedFlow()

    override val shouldEnablePurchase: Boolean = true

    private val _isPro = MutableStateFlow(false)
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun triggerPaywall() {
        if (!shouldEnablePurchase) return
        scope.launch {
            _showPaywallEvent.emit(Unit)
        }
    }

    override fun initialize() {
        // Configuration is now handled in SupabaseApplication.kt and iOSApp.swift
        scope.launch {
            checkEntitlements()
        }
    }

    override suspend fun checkEntitlements() {
        runCatching {
            logger.d { "Start configure purchases" }
            Purchases.configure(PurchasesConfiguration(BuildKonfig.REVENUECAT_API_KEY))
            Purchases.sharedInstance.awaitCustomerInfo()
        }.fold(
            onSuccess = { customerInfo -> updateProStatus(customerInfo) },
            onFailure = { error ->
                logger.e(error) { "Error checking entitlements: ${error.message}" }
                setProStatus(false)
            }
        )
    }

    override fun updatePurchaseStatus(customerInfo: com.revenuecat.purchases.kmp.models.CustomerInfo) {
        updateProStatus(customerInfo)
    }

    private fun updateProStatus(customerInfo: com.revenuecat.purchases.kmp.models.CustomerInfo) {
        val activeSubscription = customerInfo.activeSubscriptions.isNotEmpty()
        logger.d { "Updating pro status: ($activeSubscription)" }

        setProStatus(activeSubscription)
    }

    /** The one place pro status changes, so the analytics dimension is published from here. */
    private fun setProStatus(isPro: Boolean) {
        _isPro.value = isPro
        AppAnalytics.setUserProperty(AnalyticsUserProperties.IS_PRO, isPro.toString())
    }

    override fun fetchOffering(): Flow<ApiResponse<Offering>> = flow {
        emit(ApiResponse.Loading)
        val response = runCatching {
            Purchases.sharedInstance.awaitOfferings().current
                ?: throw IllegalStateException("No current offering found")
        }.onFailure { error ->
            when (error) {
                is PurchasesException -> logger.e(error) { "RevenueCat error: ${error.message}" }
                else -> logger.e(error) { "Error fetching offering: ${error.message}" }
            }
        }
        emit(ApiResponse.from(response))
    }

    override suspend fun restorePurchases(): ApiResponse<com.revenuecat.purchases.kmp.models.CustomerInfo> {
        val result = runCatching { Purchases.sharedInstance.awaitRestore() }
            .onSuccess { customerInfo -> updateProStatus(customerInfo) }
            .onFailure { error -> logger.e(error) { "Error restoring purchases: ${error.message}" } }
        return ApiResponse.from(result)
    }
}
