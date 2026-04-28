package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.usecase.ObserveProStatusUseCase
import kotlinx.coroutines.flow.Flow

class ObserveProStatusUseCaseImpl(
    private val purchaseRepository: PurchaseRepository
) : ObserveProStatusUseCase {
    override fun invoke(): Flow<Boolean> = purchaseRepository.isPro
}
