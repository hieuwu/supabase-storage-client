package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.observability.analytics.DownloadDestinationModes
import io.github.vinceglb.filekit.PlatformFile

interface DownloadFileUseCase {
    /** [destinationMode] is one of [DownloadDestinationModes] and is reported to analytics. */
    suspend fun downloadToPath(
        bucketId: String,
        path: String,
        fileName: String,
        directoryPath: String,
        destinationMode: String = DownloadDestinationModes.PICKED_FOLDER,
    )

    suspend fun download(
        bucketId: String,
        path: String,
        fileName: String,
        platformFile: PlatformFile,
        destinationMode: String = DownloadDestinationModes.PICKED_FILE,
    )
}
