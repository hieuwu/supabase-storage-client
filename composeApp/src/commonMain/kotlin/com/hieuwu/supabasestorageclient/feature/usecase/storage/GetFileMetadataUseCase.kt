package com.hieuwu.supabasestorageclient.feature.usecase.storage

import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class GetFileMetadataUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(bucketId: String, path: String): Result<StorageItem> {
        return runCatching {
            storageRepository.getFileMetadata(bucketId, path)
        }
    }
}
