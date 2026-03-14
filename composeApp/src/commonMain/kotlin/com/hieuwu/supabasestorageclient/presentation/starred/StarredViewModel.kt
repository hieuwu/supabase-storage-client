package com.hieuwu.supabasestorageclient.presentation.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StarredViewModel(
    private val starredRepository: StarredRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StarredUiState())
    val uiState: StateFlow<StarredUiState> = _uiState.asStateFlow()

    init {
        loadStarredItems()
    }

    fun loadStarredItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val starredItems = starredRepository.getStarredItems()
            val userSettings = settingsRepository.getSettings()

            combine(starredItems, userSettings) { items, settings ->
                Pair(items, settings.viewMode)
            }.collect { (items, viewMode) ->
                _uiState.update { it.copy(items = items, isLoading = false, viewMode = viewMode) }
            }
        }
    }

    fun unstarItem(id: String) {
        viewModelScope.launch {
            starredRepository.unstarItem(id)
            _uiState.update { it.copy(successMessage = "Unstarred successfully") }
        }
    }

    fun onClearAllClick() {
        _uiState.update { it.copy(showClearAllConfirmation = true) }
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            starredRepository.clearAllStarredItems()
            _uiState.update {
                it.copy(
                    showClearAllConfirmation = false,
                    successMessage = "Cleared all starred items"
                )
            }
        }
    }

    fun dismissClearAllConfirmation() {
        _uiState.update { it.copy(showClearAllConfirmation = false) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }

}
