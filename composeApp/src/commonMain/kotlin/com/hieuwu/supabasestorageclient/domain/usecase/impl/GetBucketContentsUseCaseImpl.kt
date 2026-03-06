package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.GetBucketContentsUseCase

class GetBucketContentsUseCaseImpl(
    private val storageRepository: StorageRepository
) : GetBucketContentsUseCase {
    override suspend fun invoke(params: GetBucketContentsUseCase.Params): Result<List<StorageItem>> = runCatching {
        storageRepository.getBucketContents(params.bucketId, params.path)
    }
}
