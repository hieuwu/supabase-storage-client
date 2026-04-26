package com.hieuwu.supabasestorageclient.domain.usecase

import com.hieuwu.supabasestorageclient.domain.model.StarredItem

interface ToggleStarUseCase {
    suspend operator fun invoke(item: StarredItem, isStarred: Boolean): Result<Unit>
}
