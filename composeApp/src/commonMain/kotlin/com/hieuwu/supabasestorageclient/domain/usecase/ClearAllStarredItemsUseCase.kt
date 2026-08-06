package com.hieuwu.supabasestorageclient.domain.usecase

interface ClearAllStarredItemsUseCase {
    suspend operator fun invoke(): Result<Unit>
}
