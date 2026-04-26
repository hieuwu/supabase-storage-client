package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import com.hieuwu.supabasestorageclient.domain.usecase.ToggleStarUseCase

class ToggleStarUseCaseImpl(
    private val starredRepository: StarredRepository
) : ToggleStarUseCase {
    override suspend fun invoke(item: StarredItem, isStarred: Boolean): Result<Unit> = runCatching {
        if (isStarred) {
            starredRepository.unstarItem(item.id)
        } else {
            starredRepository.starItem(item)
        }
    }
}
