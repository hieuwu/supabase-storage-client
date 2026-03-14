package com.hieuwu.supabasestorageclient.presentation.downloads

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.util.FileWriter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DownloadsViewModel(
    private val downloadManager: DownloadManager,
    private val fileWriter: FileWriter
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItem>> = downloadManager.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showDeleteConfirmationDialog = mutableStateOf<DownloadItem?>(null)
    val showDeleteConfirmationDialog: State<DownloadItem?> = _showDeleteConfirmationDialog

    private val _showCancelConfirmationDialog = mutableStateOf<DownloadItem?>(null)
    val showCancelConfirmationDialog: State<DownloadItem?> = _showCancelConfirmationDialog

    private val _showFileInfoDialog = mutableStateOf<DownloadItem?>(null)
    val showFileInfoDialog: State<DownloadItem?> = _showFileInfoDialog

    fun showFileInfo(item: DownloadItem) {
        _showFileInfoDialog.value = item
    }

    fun hideFileInfo() {
        _showFileInfoDialog.value = null
    }

    fun confirmCancel(item: DownloadItem) {
        _showCancelConfirmationDialog.value = item
    }

    fun dismissCancelConfirmation() {
        _showCancelConfirmationDialog.value = null
    }

    fun cancelConfirmed(id: String) {
        downloadManager.cancel(id)
        dismissCancelConfirmation()
    }

    fun confirmDelete(item: DownloadItem) {
        _showDeleteConfirmationDialog.value = item
    }

    fun dismissDeleteConfirmation() {
        _showDeleteConfirmationDialog.value = null
    }

    fun deleteDownload(id: String) {
        downloadManager.deleteDownload(id)
        dismissDeleteConfirmation()
    }
}
