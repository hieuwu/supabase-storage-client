package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.DeleteFileUseCase

class DeleteFileUseCaseImpl(
    private val storageRepository: StorageRepository
) : DeleteFileUseCase {
    override suspend fun invoke(params: DeleteFileUseCase.Params): Result<Unit> = runCatching {
        storageRepository.deleteFile(params.bucketId, params.path)
    }
}
