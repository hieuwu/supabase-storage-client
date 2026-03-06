package com.hieuwu.supabasestorageclient.presentation.starred

import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.model.ViewMode

data class StarredUiState(
    val items: List<StarredItem> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
    val showClearAllConfirmation: Boolean = false,
    val viewMode: ViewMode = ViewMode.LIST
)