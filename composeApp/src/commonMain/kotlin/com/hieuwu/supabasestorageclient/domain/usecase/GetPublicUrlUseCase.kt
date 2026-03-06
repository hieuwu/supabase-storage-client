package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class GetPublicUrlUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(bucketId: String, path: String): Result<String> {
        return runCatching {
            storageRepository.getPublicUrl(bucketId, path)
        }
    }
}
