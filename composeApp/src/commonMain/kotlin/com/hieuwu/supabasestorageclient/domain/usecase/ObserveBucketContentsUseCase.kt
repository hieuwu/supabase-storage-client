package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import kotlinx.coroutines.flow.Flow

interface ObserveBucketContentsUseCase {
    data class Params(val bucketId: String, val path: String)
    operator fun invoke(params: Params): Flow<Result<List<StorageItem>>>
}
