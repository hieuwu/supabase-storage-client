package com.hieuwu.supabasestorageclient.presentation.uploads

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.upload.UploadManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.compose.runtime.State

class UploadViewModel(
    private val uploadManager: UploadManager
) : ViewModel() {

    val uploads: StateFlow<List<UploadItem>> = uploadManager.uploads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showFileInfoDialog = mutableStateOf<UploadItem?>(null)
    val showFileInfoDialog: State<UploadItem?> = _showFileInfoDialog

    fun showFileInfo(item: UploadItem) {
        _showFileInfoDialog.value = item
    }

    fun hideFileInfo() {
        _showFileInfoDialog.value = null
    }

    fun cancelUpload(id: String) {
        uploadManager.cancel(id)
    }
}
