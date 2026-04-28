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

    private val _manualState = MutableStateFlow(ManualStarredState())

    val uiState: StateFlow<StarredUiState> = combine(
        getStarredItemsUseCase(),
        getUserSettingsUseCase(),
        _manualState
    ) { items, settings, manual ->
        StarredUiState.Content(
            items = items,
            viewMode = settings.viewMode,
            showClearAllConfirmation = manual.showClearAllConfirmation,
            successMessage = manual.successMessage,
            error = manual.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StarredUiState.Loading)

    fun unstarItem(id: String) {
        viewModelScope.launch {
            unstarItemUseCase(id).onSuccess {
                _manualState.update { it.copy(successMessage = "Unstarred successfully") }
            }
        }
    }

    fun onClearAllClick() {
        _manualState.update { it.copy(showClearAllConfirmation = true) }
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            clearAllStarredItemsUseCase().onSuccess {
                _manualState.update {
                    it.copy(
                        showClearAllConfirmation = false,
                        successMessage = "Cleared all starred items"
                    )
                }
            }
        }
    }

    fun dismissClearAllConfirmation() {
        _manualState.update { it.copy(showClearAllConfirmation = false) }
    }

    fun clearMessages() {
        _manualState.update { it.copy(successMessage = null, error = null) }
    }
}

data class ManualStarredState(
    val showClearAllConfirmation: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)
