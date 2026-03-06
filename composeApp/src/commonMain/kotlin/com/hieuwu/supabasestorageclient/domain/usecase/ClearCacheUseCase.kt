package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository

fun interface ClearCacheUseCase {
    data class Params(val credentialId: String? = null)
    suspend operator fun invoke(params: Params): Result<Unit>
}
