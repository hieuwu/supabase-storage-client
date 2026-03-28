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
        try {
            logger.d { "Start configure purchases" }
            Purchases.configure(PurchasesConfiguration(BuildKonfig.REVENUECAT_API_KEY))
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            updateProStatus(customerInfo)
        } catch (e: Exception) {
            logger.e(e) { "Error checking entitlements: ${e.message}" }
            _isPro.value = false
        }
    }

    override fun updatePurchaseStatus(customerInfo: com.revenuecat.purchases.kmp.models.CustomerInfo) {
        updateProStatus(customerInfo)
    }

    private fun updateProStatus(customerInfo: com.revenuecat.purchases.kmp.models.CustomerInfo) {
        val activeSubscription = customerInfo.activeSubscriptions.isNotEmpty()
        logger.d { "Updating pro status: ($activeSubscription)" }

        _isPro.value = activeSubscription
    }

    override fun fetchOffering(): Flow<ApiResponse<Offering>> = flow {
        emit(ApiResponse.Loading)
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val currentOffering = offerings.current
            if (currentOffering != null) {
                emit(ApiResponse.Success(currentOffering))
            } else {
                logger.e { "No current offering found" }
                emit(ApiResponse.Error(Exception("No current offering found")))
            }
        } catch (e: PurchasesException) {
            logger.e(e) { "RevenueCat error: ${e.message}" }
            emit(ApiResponse.Error(e))
        } catch (e: Exception) {
            logger.e(e) { "General error fetching offering: ${e.message}" }
            emit(ApiResponse.Error(e))
        }
    }
}
