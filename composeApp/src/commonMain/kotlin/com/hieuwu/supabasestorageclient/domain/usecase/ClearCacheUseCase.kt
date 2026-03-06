package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class ClearCacheUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(credentialId: String? = null) = storageRepository.clearCache(credentialId)
}
