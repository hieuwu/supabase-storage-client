package com.hieuwu.supabasestorageclient.presentation.settings

import com.hieuwu.supabasestorageclient.domain.model.UserSettings

sealed interface SettingsUiState {
    object Loading : SettingsUiState
    data class Content(
        val settings: UserSettings,
        val isPro: Boolean,
        val isRestoring: Boolean = false,
        val successMessage: String? = null,
        val error: String? = null
    ) : SettingsUiState
}
