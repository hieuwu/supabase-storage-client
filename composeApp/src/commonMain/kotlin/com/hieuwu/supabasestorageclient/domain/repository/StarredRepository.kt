package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import kotlinx.coroutines.flow.Flow

interface StarredRepository {
    fun getStarredItems(): Flow<List<StarredItem>>
    suspend fun starItem(item: StarredItem)
    suspend fun unstarItem(id: String)
    suspend fun isItemStarred(id: String): Boolean
    suspend fun clearAllStarredItems()
}
