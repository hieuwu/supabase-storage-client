package com.hieuwu.supabasestorageclient.presentation.starred

import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.model.ViewMode

sealed interface StarredUiState {
    object Loading : StarredUiState
    data class Content(
        val items: List<StarredItem> = emptyList(),
        val viewMode: ViewMode = ViewMode.LIST,
        val showClearAllConfirmation: Boolean = false,
        val successMessage: String? = null,
        val error: String? = null
    ) : StarredUiState
    data class Error(val message: String) : StarredUiState
}