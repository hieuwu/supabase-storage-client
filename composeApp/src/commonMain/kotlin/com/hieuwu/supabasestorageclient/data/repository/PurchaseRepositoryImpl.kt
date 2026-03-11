package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.BuildKonfig

import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesException
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.models.EntitlementInfo
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
        val apiKey = BuildKonfig.REVENUECAT_API_KEY
        
        if (apiKey.isNotEmpty()) {
            Purchases.logLevel = LogLevel.DEBUG
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

    override fun fetchOffering(): Flow<ApiResponse<Offering>> = flow {
        emit(ApiResponse.Loading)
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val currentOffering = offerings.current
            if (currentOffering != null) {
                emit(ApiResponse.Success(currentOffering))
            } else {
                emit(ApiResponse.Error(Exception("No current offering found")))
            }
        } catch (e: PurchasesException) {
            emit(ApiResponse.Error(e))
        } catch (e: Exception) {
            emit(ApiResponse.Error(e))
        }
    }
}
