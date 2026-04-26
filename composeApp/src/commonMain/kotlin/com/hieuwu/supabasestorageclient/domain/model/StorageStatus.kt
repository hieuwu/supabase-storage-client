package com.hieuwu.supabasestorageclient.domain.model

sealed class StorageDownloadStatus {
    data class Progress(val totalBytesReceived: Long, val contentLength: Long) : StorageDownloadStatus()
    data class ByteData(val data: ByteArray) : StorageDownloadStatus()
    object Success : StorageDownloadStatus()
}

sealed class StorageUploadStatus {
    data class Progress(val totalBytesSent: Long, val contentLength: Long) : StorageUploadStatus()
    object Success : StorageUploadStatus()
}
