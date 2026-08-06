package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.usecase.GetUserSettingsUseCase
import kotlinx.coroutines.flow.Flow

class GetUserSettingsUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : GetUserSettingsUseCase {
    override fun invoke(): Flow<UserSettings> {
        return settingsRepository.getSettings()
    }
}
