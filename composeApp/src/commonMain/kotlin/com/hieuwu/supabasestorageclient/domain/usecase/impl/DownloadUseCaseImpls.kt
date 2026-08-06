package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.usecase.CancelDownloadUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.DeleteDownloadUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.ObserveDownloadsUseCase
import kotlinx.coroutines.flow.Flow

class ObserveDownloadsUseCaseImpl(
    private val downloadManager: DownloadManager
) : ObserveDownloadsUseCase {
    override fun invoke(): Flow<List<DownloadItem>> = downloadManager.downloads
}

class CancelDownloadUseCaseImpl(
    private val downloadManager: DownloadManager
) : CancelDownloadUseCase {
    override fun invoke(id: String) = downloadManager.cancel(id)
}

class DeleteDownloadUseCaseImpl(
    private val downloadManager: DownloadManager
) : DeleteDownloadUseCase {
    override fun invoke(id: String) = downloadManager.deleteDownload(id)
}
