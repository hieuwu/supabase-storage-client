package com.hieuwu.supabasestorageclient.presentation.uploads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.usecase.ObserveUploadsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.CancelUploadUseCase
import kotlinx.coroutines.flow.*

class UploadViewModel(
    private val observeUploadsUseCase: ObserveUploadsUseCase,
    private val cancelUploadUseCase: CancelUploadUseCase
) : ViewModel() {

    private val _manualState = MutableStateFlow<UploadItem?>(null)

    val uiState: StateFlow<UploadUiState> = combine(
        observeUploadsUseCase(),
        _manualState
    ) { uploads, selectedItem ->
        if (uploads.isEmpty()) {
            UploadUiState.Empty
        } else {
            UploadUiState.Content(uploads, selectedItem)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UploadUiState.Empty)

    fun showFileInfo(item: UploadItem) {
        _manualState.value = item
    }

    fun hideFileInfo() {
        _manualState.value = null
    }

    fun cancelUpload(id: String) {
        cancelUploadUseCase(id)
    }
}
