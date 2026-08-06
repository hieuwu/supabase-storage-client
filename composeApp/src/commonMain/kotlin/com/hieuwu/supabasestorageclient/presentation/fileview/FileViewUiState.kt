package com.hieuwu.supabasestorageclient.presentation.fileview

import com.hieuwu.supabasestorageclient.domain.model.StorageItem

sealed interface FileViewUiState {
    object Loading : FileViewUiState
    data class Content(
        val bucketId: String,
        val fileName: String,
        val path: String?,
        val publicUrl: String? = null,
        val metadata: StorageItem? = null,
        val isDeleted: Boolean = false,
        val successMessage: String? = null,
        val error: String? = null,
        val itemToDownload: StorageItem? = null,
        val isPickingDirectory: Boolean = false,
        val isSavingFile: Boolean = false,
        val showDownloadPathOptionDialog: Boolean = false,
        val defaultDownloadPath: String? = null
    ) : FileViewUiState
    data class Error(val message: String) : FileViewUiState
}