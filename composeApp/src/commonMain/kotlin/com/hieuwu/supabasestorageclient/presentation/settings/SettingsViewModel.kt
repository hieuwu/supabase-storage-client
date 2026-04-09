package com.hieuwu.supabasestorageclient.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.AppTheme
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.domain.model.ViewMode
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.hieuwu.supabasestorageclient.domain.model.AskDownloadPathConfig
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.data.network.ApiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring = _isRestoring.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    val isPro = purchaseRepository.isPro

    val settings: StateFlow<UserSettings?> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun updateFileSizeLimit(limit: Long) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(current.copy(fileSizeLimit = limit))
        }
    }

    fun updateFileUnit(unit: SizeUnit) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(current.copy(fileSizeUnit = unit))
        }
    }

    fun updateViewMode(viewMode: ViewMode) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(current.copy(viewMode = viewMode))
        }
    }

    fun updateTheme(theme: AppTheme) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(current.copy(theme = theme))
        }
    }

    fun updateAskDownloadPathConfig(config: AskDownloadPathConfig) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(current.copy(askDownloadPathConfig = config))
        }
    }

    fun updateDefaultDownloadDirectory(path: String?) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(current.copy(defaultDownloadDirectory = path))
        }
    }

    fun updateSessionDownloadDirectory(path: String?) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.updateSettings(current.copy(sessionDownloadDirectory = path))
        }
    }

    fun onRestorePurchases() {
        _isRestoring.value = true
        viewModelScope.launch {
            val result = purchaseRepository.restorePurchases()
            _isRestoring.value = false
            when (result) {
                is ApiResponse.Success -> {
                    _successMessage.value = "Restored successfully!"
                }
                is ApiResponse.Error -> {
                    _error.value = "Restore failed: ${result.exception.message}"
                }
                is ApiResponse.Loading -> { }
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _successMessage.value = null
    }
}
