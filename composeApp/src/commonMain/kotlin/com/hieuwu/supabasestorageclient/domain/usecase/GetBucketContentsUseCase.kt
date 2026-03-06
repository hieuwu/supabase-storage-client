package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

fun interface GetBucketContentsUseCase {
    data class Params(val bucketId: String, val path: String)
    suspend operator fun invoke(params: Params): Result<List<StorageItem>>
}
