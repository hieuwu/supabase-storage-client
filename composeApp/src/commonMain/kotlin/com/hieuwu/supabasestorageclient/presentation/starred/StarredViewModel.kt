package com.hieuwu.supabasestorageclient.presentation.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StarredUiState(
    val items: List<StarredItem> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
    val showClearAllConfirmation: Boolean = false
)

class StarredViewModel(
    private val starredRepository: StarredRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StarredUiState())
    val uiState: StateFlow<StarredUiState> = _uiState.asStateFlow()

    init {
        loadStarredItems()
    }

    private fun loadStarredItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            starredRepository.getStarredItems()
                .collect { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                }
        }
    }

    fun unstarItem(itemId: String) {
        viewModelScope.launch {
            starredRepository.unstarItem(itemId)
            _uiState.update { it.copy(successMessage = "Unstarred successfully") }
        }
    }

    fun onClearAllClick() {
        _uiState.update { it.copy(showClearAllConfirmation = true) }
    }

    fun confirmClearAll() {
        viewModelScope.launch {
            starredRepository.clearAllStarredItems()
            _uiState.update { it.copy(showClearAllConfirmation = false, successMessage = "Cleared all starred items") }
        }
    }

    fun dismissClearAllConfirmation() {
        _uiState.update { it.copy(showClearAllConfirmation = false) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
