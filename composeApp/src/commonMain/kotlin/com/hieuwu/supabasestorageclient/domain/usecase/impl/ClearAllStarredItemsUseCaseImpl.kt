package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import com.hieuwu.supabasestorageclient.domain.usecase.ClearAllStarredItemsUseCase

class ClearAllStarredItemsUseCaseImpl(
    private val starredRepository: StarredRepository
) : ClearAllStarredItemsUseCase {
    override suspend fun invoke(): Result<Unit> = runCatching {
        starredRepository.clearAllStarredItems()
    }
}
