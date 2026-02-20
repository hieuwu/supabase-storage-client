package com.hieuwu.supabasestorageclient.presentation.downloads

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

    fun pauseDownload(id: String) {
        downloadManager.pause(id)
    }

    fun resumeDownload(id: String) {
        downloadManager.resume(id)
    }

    fun cancelDownload(id: String) {
        downloadManager.cancel(id)
    }

    fun openFile(item: DownloadItem) {
        val fullPath = if (item.destinationPath.endsWith("/")) {
            item.destinationPath + item.fileName
        } else {
            "${item.destinationPath}/${item.fileName}"
        }
        
        if (fileWriter.exists(fullPath)) {
            fileOpener.openFile(fullPath)
        } else {
            // In a real app, we'd use a UI effect or a Snackbar to notify the user
            println("File does not exist: $fullPath")
        }
    }
}
