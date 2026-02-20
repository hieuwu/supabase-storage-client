package com.hieuwu.supabasestorageclient.feature.usecase.storage

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class DeleteBucketUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(bucketId: String): Result<Unit> {
        return runCatching {
            storageRepository.deleteBucket(bucketId)
        }
    }
}
