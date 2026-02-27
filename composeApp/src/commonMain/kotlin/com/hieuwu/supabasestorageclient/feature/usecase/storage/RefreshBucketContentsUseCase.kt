package com.hieuwu.supabasestorageclient.feature.usecase.storage

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class RefreshBucketContentsUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(bucketId: String) = runCatching {
        storageRepository.clearContentsCache(bucketId)
    }
}
