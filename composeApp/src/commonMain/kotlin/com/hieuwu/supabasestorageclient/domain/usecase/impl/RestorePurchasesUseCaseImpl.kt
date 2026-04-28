package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.usecase.RestorePurchasesUseCase

class RestorePurchasesUseCaseImpl(
    private val purchaseRepository: PurchaseRepository
) : RestorePurchasesUseCase {
    override suspend fun invoke(): ApiResponse<Unit> {
        val result = purchaseRepository.restorePurchases()
        return when (result) {
            is ApiResponse.Success -> ApiResponse.Success(Unit)
            is ApiResponse.Error -> ApiResponse.Error(result.exception)
            is ApiResponse.Loading -> ApiResponse.Loading
        }
    }
}
