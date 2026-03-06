package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

fun interface GetBucketsUseCase {
    suspend operator fun invoke(): Result<List<Bucket>>
}
