package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class DeleteFileUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(bucketId: String, path: String): Result<Unit> {
        return runCatching {
            storageRepository.deleteFile(bucketId, path)
        }
    }
}
