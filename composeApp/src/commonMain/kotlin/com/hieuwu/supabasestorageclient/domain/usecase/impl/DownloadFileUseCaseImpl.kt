package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.DownloadFileUseCase

class DownloadFileUseCaseImpl(
    private val storageRepository: StorageRepository
) : DownloadFileUseCase {
    override suspend fun invoke(params: DownloadFileUseCase.Params): Result<ByteArray> = runCatching {
        storageRepository.downloadFile(params.bucketId, params.path)
    }
}
