package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import com.hieuwu.supabasestorageclient.domain.usecase.UnstarItemUseCase

class UnstarItemUseCaseImpl(
    private val starredRepository: StarredRepository
) : UnstarItemUseCase {
    override suspend fun invoke(id: String): Result<Unit> = runCatching {
        starredRepository.unstarItem(id)
    }
}
