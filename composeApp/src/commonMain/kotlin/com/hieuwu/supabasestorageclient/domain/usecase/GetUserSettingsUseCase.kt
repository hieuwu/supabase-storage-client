package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface GetUserSettingsUseCase {
    operator fun invoke(): Flow<UserSettings>
}
