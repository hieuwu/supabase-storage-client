package com.hieuwu.supabasestorageclient.presentation.fileview

import com.hieuwu.supabasestorageclient.domain.model.StorageItem

data class FileViewUiState(
    val bucketId: String = "",
    val fileName: String = "",
    val path: String? = null,
    val publicUrl: String? = null,
    val metadata: StorageItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val successMessage: String? = null,
    val itemToDownload: StorageItem? = null,
    val isPickingDirectory: Boolean = false,
    val isSavingFile: Boolean = false,
    val showDownloadPathOptionDialog: Boolean = false,
    val defaultDownloadPath: String? = null
)