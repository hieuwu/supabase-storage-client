package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class EmptyBucketUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(bucketId: String): Result<Unit> {
        return runCatching {
            storageRepository.emptyBucket(bucketId)
        }
    }
}
