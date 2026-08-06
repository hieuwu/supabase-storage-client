package com.hieuwu.supabasestorageclient.presentation.formatters

import com.hieuwu.supabasestorageclient.core.format
import com.hieuwu.supabasestorageclient.core.formatDateTime
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.model.UploadStatus

fun DownloadItem.formatStatus(): String = when (status) {
    DownloadStatus.Downloading -> "Downloading..."
    DownloadStatus.Paused -> "Paused"
    DownloadStatus.Completed -> "Completed"
    DownloadStatus.Error -> "Error"
    DownloadStatus.Cancelled -> "Cancelled"
}

fun DownloadItem.formatDownloadProgress(): String {
    if (downloadedSize <= 0) return formatBytes(totalSize)
    return "${formatBytes(downloadedSize)} / ${formatBytes(totalSize)}"
}

fun UploadItem.formatUploadProgress(): String {
    if (totalSize <= 0) return formatBytes(this.uploadedSize)
    return "${formatBytes(uploadedSize)} / ${formatBytes(totalSize)}"
}

fun UploadItem.formatStatus(): String = when (status) {
    UploadStatus.Uploading -> "Uploading..."
    UploadStatus.Paused -> "Paused"
    UploadStatus.Completed -> "Completed ${
        if (uploadedTime != null) "at ${
            formatDateTime(
                uploadedTime
            )
        }" else ""
    }"
    UploadStatus.Error -> "Error"
    UploadStatus.Cancelled -> "Cancelled"
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.format(1)} KB"
    val mb = kb / 1024.0
    return "${mb.format(1)} MB"
}