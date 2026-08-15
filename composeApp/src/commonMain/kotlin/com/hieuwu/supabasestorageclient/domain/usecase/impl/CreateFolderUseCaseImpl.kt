package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.CreateFolderUseCase

class CreateFolderUseCaseImpl(
    private val repository: StorageRepository
) : CreateFolderUseCase {
    override suspend fun invoke(params: CreateFolderUseCase.Params): Result<Unit> =
        repository.createFolder(params.bucketId, params.path)
}
