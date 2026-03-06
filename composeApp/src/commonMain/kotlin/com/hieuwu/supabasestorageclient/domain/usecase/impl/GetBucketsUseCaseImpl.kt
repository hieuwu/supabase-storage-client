package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.GetBucketsUseCase

class GetBucketsUseCaseImpl(
    private val storageRepository: StorageRepository
) : GetBucketsUseCase {
    override suspend fun invoke(): Result<List<Bucket>> = runCatching {
        storageRepository.getBuckets()
    }
}
