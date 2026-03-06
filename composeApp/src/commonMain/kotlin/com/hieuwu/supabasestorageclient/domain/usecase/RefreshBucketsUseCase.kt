package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

fun interface RefreshBucketsUseCase {
    suspend operator fun invoke(): Result<Unit>
}
