package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.RefreshBucketsUseCase

class RefreshBucketsUseCaseImpl(
    private val storageRepository: StorageRepository
) : RefreshBucketsUseCase {
    override suspend fun invoke(): Result<Unit> =
        storageRepository.clearBucketsCache()
}
