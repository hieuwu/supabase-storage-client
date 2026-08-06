package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.UserSettings

interface UpdateUserSettingsUseCase {
    suspend operator fun invoke(settings: UserSettings)
}
