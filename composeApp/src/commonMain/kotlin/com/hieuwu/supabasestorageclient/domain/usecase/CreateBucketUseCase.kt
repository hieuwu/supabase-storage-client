package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class CreateBucketUseCase(
    private val storageRepository: StorageRepository
) {
    suspend operator fun invoke(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?) = runCatching {
        storageRepository.createBucket(id, public, fileSizeLimit, unit)
    }
}
