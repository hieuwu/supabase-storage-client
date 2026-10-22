package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.usecase.DownloadFileUseCase
import io.github.vinceglb.filekit.PlatformFile

class DownloadFileUseCaseImpl(
    private val downloadManager: DownloadManager
) : DownloadFileUseCase {
    override suspend fun downloadToPath(
        bucketId: String,
        path: String,
        fileName: String,
        directoryPath: String,
        destinationMode: String,
    ) {
        downloadManager.downloadToDirectoryPath(bucketId, path, fileName, directoryPath, destinationMode)
    }

    override suspend fun download(
        bucketId: String,
        path: String,
        fileName: String,
        platformFile: PlatformFile,
        destinationMode: String,
    ) {
        downloadManager.download(bucketId, path, fileName, platformFile, destinationMode)
    }
}
