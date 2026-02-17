package com.hieuwu.supabasestorageclient.domain.repository

/**
 * Repository interface for managing onboarding state
 */
interface OnboardingRepository {
    /**
     * Check if user has completed onboarding
     * @return true if onboarding is completed, false otherwise
     */
    fun isOnboardingCompleted(): Boolean
    
    /**
     * Mark onboarding as completed
     */
    fun markOnboardingCompleted()
}
