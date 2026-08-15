package com.hieuwu.supabasestorageclient.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.*
import com.hieuwu.supabasestorageclient.domain.usecase.*
import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import co.touchlab.kermit.Logger
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
        update { it.copy(fileSizeLimit = limit) }
    }

    fun updateFileUnit(unit: SizeUnit) {
        update { it.copy(fileSizeUnit = unit) }
    }

    fun updateViewMode(viewMode: ViewMode) {
        update { it.copy(viewMode = viewMode) }
    }

    fun updateTheme(theme: AppTheme) {
        update { it.copy(theme = theme) }
    }

    fun updateAskDownloadPathConfig(config: AskDownloadPathConfig) {
        update { it.copy(askDownloadPathConfig = config) }
    }

    fun updateDefaultDownloadDirectory(path: String?) {
        update { it.copy(defaultDownloadDirectory = path) }
    }

    fun updateSessionDownloadDirectory(path: String?) {
        update { it.copy(sessionDownloadDirectory = path) }
    }

    private fun update(block: (UserSettings) -> UserSettings) {
        val currentState = uiState.value as? SettingsUiState.Content ?: return
        viewModelScope.launch {
            updateUserSettingsUseCase(block(currentState.settings)).onFailure { error ->
                logger.e(error) { "Failed to save settings" }
                _manualState.update { it.copy(error = "Could not save the setting") }
            }
        }
    }

    fun onRestorePurchases() {
        _manualState.update { it.copy(isRestoring = true, error = null) }
        viewModelScope.launch {
            val result = restorePurchasesUseCase()
            _manualState.update { it.copy(isRestoring = false) }
            when (result) {
                is ApiResponse.Success -> {
                    _manualState.update { it.copy(successMessage = "Restored successfully!") }
                }
                is ApiResponse.Error -> {
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
