package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.EmptyBucketUseCase

class EmptyBucketUseCaseImpl(
    private val storageRepository: StorageRepository
) : EmptyBucketUseCase {
    override suspend fun invoke(bucketId: String): Result<Unit> = runCatching {
        storageRepository.emptyBucket(bucketId)
    }
}
