package com.hieuwu.supabasestorageclient.presentation.uploads

import com.hieuwu.supabasestorageclient.domain.model.UploadItem

sealed interface UploadUiState {
    object Empty : UploadUiState
    data class Content(
        val uploads: List<UploadItem>,
        val selectedItem: UploadItem? = null
    ) : UploadUiState
}
