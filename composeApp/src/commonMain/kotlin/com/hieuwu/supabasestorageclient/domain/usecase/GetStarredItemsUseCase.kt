package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import kotlinx.coroutines.flow.Flow

interface GetStarredItemsUseCase {
    operator fun invoke(): Flow<List<StarredItem>>
}
