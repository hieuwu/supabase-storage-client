package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class CreateFolderUseCase(private val repository: StorageRepository) {
    suspend operator fun invoke(bucketId: String, path: String): Result<Unit> {
        return try {
            repository.createFolder(bucketId, path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
