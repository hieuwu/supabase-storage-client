package com.hieuwu.supabasestorageclient.feature.usecase.storage

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class MoveFileUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(bucketId: String, fromPath: String, toPath: String): Result<Unit> {
        return runCatching {
            storageRepository.moveFile(bucketId, fromPath, toPath)
        }
    }
}
