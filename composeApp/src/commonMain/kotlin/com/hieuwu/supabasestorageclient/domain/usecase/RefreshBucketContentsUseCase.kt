package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

fun interface RefreshBucketContentsUseCase {
    suspend operator fun invoke(bucketId: String): Result<Unit>
}
