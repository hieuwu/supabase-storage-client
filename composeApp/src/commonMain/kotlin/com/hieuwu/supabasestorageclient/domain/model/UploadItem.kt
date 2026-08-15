package com.hieuwu.supabasestorageclient.domain.model

import kotlinx.datetime.Instant

data class UploadItem(
    val id: String,
    val fileName: String,
    val bucketId: String,
    val path: String,
    val totalSize: Long,
    val uploadedSize: Long = 0,
    val status: UploadStatus = UploadStatus.Uploading,
    val uploadedTime: Instant? = null,
    val from: String,
    val to: String,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (totalSize > 0) uploadedSize.toFloat() / totalSize else 0f
}

enum class UploadStatus {
    Uploading,
    Paused,
    Completed,
    Error,
    Cancelled
}
