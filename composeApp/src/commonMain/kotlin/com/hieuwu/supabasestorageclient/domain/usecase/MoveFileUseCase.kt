package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

fun interface MoveFileUseCase {
    data class Params(val bucketId: String, val fromPath: String, val toPath: String)
    suspend operator fun invoke(params: Params): Result<Unit>
}
