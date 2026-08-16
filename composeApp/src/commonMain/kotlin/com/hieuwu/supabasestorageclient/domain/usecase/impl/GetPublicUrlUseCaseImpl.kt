package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.GetPublicUrlUseCase

class GetPublicUrlUseCaseImpl(
    private val storageRepository: StorageRepository
) : GetPublicUrlUseCase {
    override suspend fun invoke(params: GetPublicUrlUseCase.Params): Result<String> =
        storageRepository.getPublicUrl(params.bucketId, params.path)
}
