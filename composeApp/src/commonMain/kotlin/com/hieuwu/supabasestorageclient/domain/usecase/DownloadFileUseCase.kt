package com.hieuwu.supabasestorageclient.domain.usecase

import io.github.vinceglb.filekit.PlatformFile

interface DownloadFileUseCase {
    suspend fun downloadToPath(bucketId: String, path: String, fileName: String, directoryPath: String)
    suspend fun download(bucketId: String, path: String, fileName: String, platformFile: PlatformFile)
}
