package com.hieuwu.supabasestorageclient.presentation.filebrowser

import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.model.ViewMode

data class BucketUiState(
    val items: List<StorageItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val viewMode: ViewMode = ViewMode.LIST
)