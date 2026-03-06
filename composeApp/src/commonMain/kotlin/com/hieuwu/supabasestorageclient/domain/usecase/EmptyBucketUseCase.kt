package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

fun interface EmptyBucketUseCase {
    suspend operator fun invoke(bucketId: String): Result<Unit>
}
