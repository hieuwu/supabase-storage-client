package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import kotlinx.coroutines.flow.Flow

interface StarredRepository {
    fun getStarredItems(): Flow<List<StarredItem>>
    suspend fun starItem(item: StarredItem)
    suspend fun unstarItem(itemId: String)
    suspend fun isItemStarred(itemId: String): Boolean
    suspend fun clearAllStarredItems()
}
