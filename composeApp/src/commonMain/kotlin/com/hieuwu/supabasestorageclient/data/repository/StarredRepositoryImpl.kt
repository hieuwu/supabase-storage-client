package com.hieuwu.supabasestorageclient.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.hieuwu.supabasestorageclient.database.AppDatabase
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class StarredRepositoryImpl(
    private val appDatabase: AppDatabase,
    private val credentialRepository: CredentialRepository
) : StarredRepository {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getStarredItems(): Flow<List<StarredItem>> {
        return credentialRepository.lastUsedId.flatMapLatest { credentialId ->
            if (credentialId == null) return@flatMapLatest emptyFlow()
            
            appDatabase.appDatabaseQueries
                .getStarredItems(credentialId)
                .asFlow()
                .mapToList(Dispatchers.Default)
                .map { entities ->
                    entities.map { entity ->
                        StarredItem(
                            itemId = entity.item_id,
                            itemName = entity.item_name,
                            bucketId = entity.bucket_id,
                            path = entity.path,
                            isFolder = entity.is_folder == 1L,
                            isBucket = entity.is_bucket == 1L,
                            starredAt = entity.starred_at
                        )
                    }
                }
        }
    }

    override suspend fun starItem(item: StarredItem) {
        val credentialId = credentialRepository.getLastUsedId() ?: return
        appDatabase.appDatabaseQueries.insertStarredItem(
            credential_id = credentialId,
            item_id = item.itemId,
            item_name = item.itemName,
            bucket_id = item.bucketId,
            path = item.path,
            is_folder = if (item.isFolder) 1L else 0L,
            is_bucket = if (item.isBucket) 1L else 0L,
            starred_at = item.starredAt
        )
    }

    override suspend fun unstarItem(itemId: String) {
        val credentialId = credentialRepository.getLastUsedId() ?: return
        appDatabase.appDatabaseQueries.deleteStarredItem(credentialId, itemId)
    }

    override suspend fun isItemStarred(itemId: String): Boolean {
        val credentialId = credentialRepository.getLastUsedId() ?: return false
        return appDatabase.appDatabaseQueries.isItemStarred(credentialId, itemId).executeAsOne() > 0
    }

    override suspend fun clearAllStarredItems() {
        val credentialId = credentialRepository.getLastUsedId() ?: return
        appDatabase.appDatabaseQueries.deleteAllStarredItems(credentialId)
    }
}
