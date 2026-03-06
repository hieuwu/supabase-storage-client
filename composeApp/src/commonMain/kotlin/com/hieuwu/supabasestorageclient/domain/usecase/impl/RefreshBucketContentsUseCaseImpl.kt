package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.RefreshBucketContentsUseCase

class RefreshBucketContentsUseCaseImpl(
    private val storageRepository: StorageRepository
) : RefreshBucketContentsUseCase {
    override suspend fun invoke(bucketId: String): Result<Unit> = runCatching {
        storageRepository.clearContentsCache(bucketId)
    }
}
