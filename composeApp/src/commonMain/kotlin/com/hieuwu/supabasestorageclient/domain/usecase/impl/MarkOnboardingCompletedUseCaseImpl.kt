package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.OnboardingRepository
import com.hieuwu.supabasestorageclient.domain.usecase.MarkOnboardingCompletedUseCase

class MarkOnboardingCompletedUseCaseImpl(
    private val onboardingRepository: OnboardingRepository
) : MarkOnboardingCompletedUseCase {
    override suspend fun invoke(): Result<Unit> =
        onboardingRepository.markOnboardingCompleted()
}
