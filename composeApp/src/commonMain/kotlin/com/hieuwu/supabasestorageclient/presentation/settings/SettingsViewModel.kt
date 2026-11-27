package com.hieuwu.supabasestorageclient.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.*
import com.hieuwu.supabasestorageclient.domain.usecase.*
import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.observability.analytics.AnalyticsSources
import com.hieuwu.supabasestorageclient.observability.analytics.AppAnalytics
import com.hieuwu.supabasestorageclient.observability.analytics.SettingKeys
import com.hieuwu.supabasestorageclient.observability.analytics.logRestoreCompleted
import com.hieuwu.supabasestorageclient.observability.analytics.logRestoreFailed
import com.hieuwu.supabasestorageclient.observability.analytics.logRestoreStarted
import com.hieuwu.supabasestorageclient.observability.analytics.logSettingChanged
import com.hieuwu.supabasestorageclient.observability.analytics.purchaseErrorReason
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val observeProStatusUseCase: ObserveProStatusUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val logger: Logger
) : ViewModel() {

    private val _manualState = MutableStateFlow(ManualSettingsState())

    val uiState: StateFlow<SettingsUiState> = combine(
        getUserSettingsUseCase(),
        observeProStatusUseCase(),
        _manualState
    ) { settings, isPro, manual ->
        SettingsUiState.Content(
            settings = settings,
            isPro = isPro,
            isRestoring = manual.isRestoring,
            successMessage = manual.successMessage,
            error = manual.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState.Loading)

    fun updateFileSizeLimit(limit: Long) {
        // The number itself is a user choice, not an identifier - but it is unbounded, so only the
        // fact that the default was changed is reported.
        update(SettingKeys.FILE_SIZE_LIMIT, "custom") { it.copy(fileSizeLimit = limit) }
    }

    fun updateFileUnit(unit: SizeUnit) {
        update(SettingKeys.FILE_SIZE_UNIT, unit.name.lowercase()) { it.copy(fileSizeUnit = unit) }
    }

    fun updateViewMode(viewMode: ViewMode) {
        update(SettingKeys.VIEW_MODE, viewMode.name.lowercase()) { it.copy(viewMode = viewMode) }
    }

    fun updateTheme(theme: AppTheme) {
        update(SettingKeys.THEME, theme.name.lowercase()) { it.copy(theme = theme) }
    }

    fun updateAskDownloadPathConfig(config: AskDownloadPathConfig) {
        update(SettingKeys.DOWNLOAD_PATH_MODE, config.name.lowercase()) {
            it.copy(askDownloadPathConfig = config)
        }
    }

    fun updateDefaultDownloadDirectory(path: String?) {
        // Only whether one is set - the path itself names the user's machine.
        update(SettingKeys.DEFAULT_DOWNLOAD_DIRECTORY, if (path == null) "cleared" else "set") {
            it.copy(defaultDownloadDirectory = path)
        }
    }

    fun updateSessionDownloadDirectory(path: String?) {
        // Not a user-visible setting - it is remembered as a side effect of a download.
        update(setting = null, value = null) { it.copy(sessionDownloadDirectory = path) }
    }

    private fun update(setting: String?, value: String?, block: (UserSettings) -> UserSettings) {
        val currentState = uiState.value as? SettingsUiState.Content ?: return
        viewModelScope.launch {
            updateUserSettingsUseCase(block(currentState.settings)).fold(
                onSuccess = {
                    if (setting != null && value != null) {
                        AppAnalytics.logSettingChanged(setting, value)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save settings" }
                    _manualState.update { it.copy(error = "Could not save the setting") }
                }
            )
        }
    }

    fun onRestorePurchases() {
        _manualState.update { it.copy(isRestoring = true, error = null) }
        AppAnalytics.logRestoreStarted(AnalyticsSources.SETTINGS)
        viewModelScope.launch {
            val result = restorePurchasesUseCase()
            _manualState.update { it.copy(isRestoring = false) }
            when (result) {
                is ApiResponse.Success -> {
                    // The repository refreshes pro status before returning, so this reads the
                    // outcome of the restore, not the state from before it.
                    AppAnalytics.logRestoreCompleted(
                        source = AnalyticsSources.SETTINGS,
                        isPro = observeProStatusUseCase().first(),
                    )
                    _manualState.update { it.copy(successMessage = "Restored successfully!") }
                }
                is ApiResponse.Error -> {
                    AppAnalytics.logRestoreFailed(
                        source = AnalyticsSources.SETTINGS,
                        reason = purchaseErrorReason(result.exception.message),
                    )
                    _manualState.update { it.copy(error = "Restore failed: ${result.exception.message}") }
                }
                is ApiResponse.Loading -> { }
            }
        }
    }

    fun clearMessages() {
        _manualState.update { it.copy(error = null, successMessage = null) }
    }
}

data class ManualSettingsState(
    val isRestoring: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)
