package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.OnboardingRepository
import com.hieuwu.supabasestorageclient.domain.usecase.IsOnboardingCompletedUseCase

class IsOnboardingCompletedUseCaseImpl(
    private val onboardingRepository: OnboardingRepository
) : IsOnboardingCompletedUseCase {
    override fun invoke(): Boolean = onboardingRepository.isOnboardingCompleted()
}
