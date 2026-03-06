package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class GetBucketsUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(): Result<List<Bucket>> {
        return runCatching {
            storageRepository.getBuckets()
        }
    }
}
