package com.hieuwu.supabasestorageclient.feature.usecase.storage

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class DownloadFileUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(bucketId: String, path: String): Result<ByteArray> {
        return runCatching {
            storageRepository.downloadFile(bucketId, path)
        }
    }
}
