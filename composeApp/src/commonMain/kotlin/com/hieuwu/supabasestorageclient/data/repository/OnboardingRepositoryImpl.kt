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
        // A read failure must not block app start - showing onboarding again is the safe fallback.
        return runCatching { settings.getBoolean(KEY_ONBOARDING_COMPLETED, false) }
            .getOrElse { error ->
                logger.e(error) { "Failed to read onboarding state, treating it as not completed" }
                false
            }
    }

    override fun markOnboardingCompleted(): Result<Unit> =
        runCatching { settings[KEY_ONBOARDING_COMPLETED] = true }
            .onFailure { error -> logger.e(error) { "Failed to persist onboarding completion" } }
}
