package com.hieuwu.supabasestorageclient.domain.model

import kotlinx.datetime.Instant

data class DownloadItem(
    val id: String,
    val fileName: String,
    val bucketId: String,
    val path: String,
    val from: String,
    val totalSize: Long,
    val downloadedSize: Long = 0,
    val status: DownloadStatus = DownloadStatus.Downloading,
    val downloadedTime: Instant? = null,
    val destinationPath: String,
    val sourcePath: String,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (totalSize > 0) downloadedSize.toFloat() / totalSize else 0f
}

enum class DownloadStatus {
    Downloading,
    Paused,
    Completed,
    Error,
    Cancelled
}
