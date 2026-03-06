package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.DeleteBucketUseCase

class DeleteBucketUseCaseImpl(
    private val storageRepository: StorageRepository
) : DeleteBucketUseCase {
    override suspend fun invoke(bucketId: String): Result<Unit> = runCatching {
        storageRepository.deleteBucket(bucketId)
    }
}
