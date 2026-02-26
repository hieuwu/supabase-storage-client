package com.hieuwu.supabasestorageclient.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
}
