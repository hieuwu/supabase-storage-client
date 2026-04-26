package com.hieuwu.supabasestorageclient.presentation.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.usecase.GetStarredItemsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetUserSettingsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.UnstarItemUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.ClearAllStarredItemsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StarredViewModel(
    private val getStarredItemsUseCase: GetStarredItemsUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val unstarItemUseCase: UnstarItemUseCase,
    private val clearAllStarredItemsUseCase: ClearAllStarredItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StarredUiState())
    val uiState: StateFlow<StarredUiState> = _uiState.asStateFlow()

    init {
        loadStarredItems()
    }

    fun loadStarredItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val starredItems = getStarredItemsUseCase()
            val userSettings = getUserSettingsUseCase()

            combine(starredItems, userSettings) { items, settings ->
                Pair(items, settings.viewMode)
            }.collect { (items, viewMode) ->
                _uiState.update { it.copy(items = items, isLoading = false, viewMode = viewMode) }
            }
        }
    }

    fun unstarItem(id: String) {
        viewModelScope.launch {
            unstarItemUseCase(id).onSuccess {
                _uiState.update { it.copy(successMessage = "Unstarred successfully") }
            }
        }
    }

    fun onClearAllClick() {
        _uiState.update { it.copy(showClearAllConfirmation = true) }
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            clearAllStarredItemsUseCase().onSuccess {
                _uiState.update {
                    it.copy(
                        showClearAllConfirmation = false,
                        successMessage = "Cleared all starred items"
                    )
                }
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
