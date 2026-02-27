package com.hieuwu.supabasestorageclient.feature.usecase.storage

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

class ClearCacheUseCase(private val storageRepository: StorageRepository) {
    suspend operator fun invoke(credentialId: String? = null) = storageRepository.clearCache(credentialId)
}
