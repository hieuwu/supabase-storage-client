package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.usecase.TriggerPaywallUseCase

class TriggerPaywallUseCaseImpl(
    private val purchaseRepository: PurchaseRepository
) : TriggerPaywallUseCase {
    override fun invoke() = purchaseRepository.triggerPaywall()
}
