package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.MoveFileUseCase

class MoveFileUseCaseImpl(
    private val storageRepository: StorageRepository
) : MoveFileUseCase {
    override suspend fun invoke(params: MoveFileUseCase.Params): Result<Unit> =
        storageRepository.moveFile(params.bucketId, params.fromPath, params.toPath)
}
