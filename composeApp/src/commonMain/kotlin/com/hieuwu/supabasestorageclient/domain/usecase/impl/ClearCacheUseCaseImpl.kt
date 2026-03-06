package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.ClearCacheUseCase

class ClearCacheUseCaseImpl(
    private val storageRepository: StorageRepository
) : ClearCacheUseCase {
    override suspend fun invoke(params: ClearCacheUseCase.Params): Result<Unit> = runCatching {
        storageRepository.clearCache(params.credentialId)
    }
}
