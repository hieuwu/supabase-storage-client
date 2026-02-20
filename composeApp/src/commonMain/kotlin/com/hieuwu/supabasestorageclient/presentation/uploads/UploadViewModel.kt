package com.hieuwu.supabasestorageclient.presentation.uploads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.upload.UploadManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class UploadViewModel(
    private val uploadManager: UploadManager
) : ViewModel() {

    val uploads: StateFlow<List<UploadItem>> = uploadManager.uploads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancelUpload(id: String) {
        uploadManager.cancel(id)
    }
}
