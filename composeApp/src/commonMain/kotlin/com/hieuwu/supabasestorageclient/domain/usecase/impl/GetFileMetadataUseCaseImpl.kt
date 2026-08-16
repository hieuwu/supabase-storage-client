package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.usecase.GetFileMetadataUseCase

class GetFileMetadataUseCaseImpl(
    private val storageRepository: StorageRepository
) : GetFileMetadataUseCase {
    override suspend fun invoke(params: GetFileMetadataUseCase.Params): Result<StorageItem> =
        storageRepository.getFileMetadata(params.bucketId, params.path)
}
