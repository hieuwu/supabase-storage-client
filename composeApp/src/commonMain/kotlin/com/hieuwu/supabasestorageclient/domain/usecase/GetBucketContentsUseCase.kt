package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class GetBucketContentsUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(bucketId: String, path: String): Result<List<StorageItem>> {
        return runCatching {
            storageRepository.getBucketContents(bucketId, path)
        }
    }
}
