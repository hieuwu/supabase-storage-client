package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.CreateBucketUseCase

class CreateBucketUseCaseImpl(
    private val storageRepository: StorageRepository
) : CreateBucketUseCase {
    override suspend fun invoke(params: CreateBucketUseCase.Params): Result<Unit> =
        storageRepository.createBucket(
            id = params.id,
            public = params.public,
            fileSizeLimit = params.fileSizeLimit,
            unit = params.unit
        )
}
