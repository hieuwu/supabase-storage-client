package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.usecase.UpdateUserSettingsUseCase

class UpdateUserSettingsUseCaseImpl(
    private val settingsRepository: SettingsRepository
) : UpdateUserSettingsUseCase {
    override suspend fun invoke(settings: UserSettings): Result<Unit> =
        settingsRepository.updateSettings(settings)
}
