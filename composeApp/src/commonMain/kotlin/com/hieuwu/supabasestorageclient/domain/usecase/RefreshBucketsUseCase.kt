package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class RefreshBucketsUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke() = runCatching {
        storageRepository.clearBucketsCache()
    }
}
