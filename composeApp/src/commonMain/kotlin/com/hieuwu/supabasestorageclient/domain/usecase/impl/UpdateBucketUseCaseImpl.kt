package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.UpdateBucketUseCase

class UpdateBucketUseCaseImpl(
    private val storageRepository: StorageRepository
) : UpdateBucketUseCase {
    override suspend fun invoke(params: UpdateBucketUseCase.Params): Result<Unit> =
        storageRepository.updateBucket(
            id = params.id,
            public = params.public,
            fileSizeLimit = params.fileSizeLimit,
            unit = params.unit
        )
}
