package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow

interface ObserveDownloadsUseCase {
    operator fun invoke(): Flow<List<DownloadItem>>
}

interface CancelDownloadUseCase {
    operator fun invoke(id: String)
}

interface DeleteDownloadUseCase {
    operator fun invoke(id: String)
}
