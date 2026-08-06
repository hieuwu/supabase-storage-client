package com.hieuwu.supabasestorageclient.domain.usecase.impl

import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import com.hieuwu.supabasestorageclient.domain.usecase.GetStarredItemsUseCase
import kotlinx.coroutines.flow.Flow

class GetStarredItemsUseCaseImpl(
    private val starredRepository: StarredRepository
) : GetStarredItemsUseCase {
    override fun invoke(): Flow<List<StarredItem>> {
        return starredRepository.getStarredItems()
    }
}
