package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.domain.repository.OnboardingRepository
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * Implementation of OnboardingRepository using multiplatform-settings
 * Storage is encrypted on Android (EncryptedSharedPreferences),
 * uses Keychain on iOS, and StorageSettings on WASM
 */
class OnboardingRepositoryImpl(
    private val settings: Settings,
    private val logger: Logger
) : OnboardingRepository {
    
    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
    
    override fun isOnboardingCompleted(): Boolean {
        return settings.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    override fun markOnboardingCompleted() {
        settings[KEY_ONBOARDING_COMPLETED] = true
    }
}
