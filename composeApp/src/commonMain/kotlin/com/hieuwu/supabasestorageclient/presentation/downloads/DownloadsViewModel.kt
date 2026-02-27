package com.hieuwu.supabasestorageclient.presentation.downloads

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.util.FileOpener
import com.hieuwu.supabasestorageclient.util.FileWriter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DownloadsViewModel(
    private val downloadManager: DownloadManager,
    private val fileOpener: FileOpener,
    private val fileWriter: FileWriter
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItem>> = downloadManager.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showDeleteConfirmationDialog = mutableStateOf<DownloadItem?>(null)
    val showDeleteConfirmationDialog: State<DownloadItem?> = _showDeleteConfirmationDialog

    fun pauseDownload(id: String) {
        downloadManager.pause(id)
    }

    fun resumeDownload(id: String) {
        downloadManager.resume(id)
    }

    fun cancelDownload(id: String) {
        downloadManager.cancel(id)
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

    fun openFile(item: DownloadItem) {
        val fullPath = getFullPath(item)
        if (fileWriter.exists(fullPath)) {
            fileOpener.openFile(fullPath)
        } else {
            println("File does not exist: $fullPath")
        }
    }

    fun openDirectory(item: DownloadItem) {
        val fullPath = getFullPath(item)
        // Note: openDirectory in our interface takes the path to the file/folder 
        // and should handle finding the parent if needed, but we can be explicit.
        fileOpener.openDirectory(fullPath)
    }

    private fun getFullPath(item: DownloadItem): String {
        return if (item.destinationPath.endsWith("/")) {
            item.destinationPath + item.fileName
        } else {
            "${item.destinationPath}/${item.fileName}"
        }
    }
}
