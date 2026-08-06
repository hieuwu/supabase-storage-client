package com.hieuwu.supabasestorageclient.domain.usecase

interface UnstarItemUseCase {
    suspend operator fun invoke(id: String): Result<Unit>
}
