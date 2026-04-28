package com.hieuwu.supabasestorageclient.presentation.filebrowser

import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.model.ViewMode

sealed interface BucketUiState {
    object Loading : BucketUiState
    data class Content(
        val items: List<StorageItem> = emptyList(),
        val viewMode: ViewMode = ViewMode.LIST,
        val itemToDownload: StorageItem? = null,
        val isPickingDirectory: Boolean = false,
        val isSavingFile: Boolean = false,
        val showDownloadPathOptionDialog: Boolean = false,
        val defaultDownloadPath: String? = null,
        val successMessage: String? = null,
        val error: String? = null
    ) : BucketUiState
    data class Error(val message: String) : BucketUiState
}