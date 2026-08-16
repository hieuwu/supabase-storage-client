package com.hieuwu.supabasestorageclient.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.hieuwu.supabasestorageclient.database.AppDatabase
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class StarredRepositoryImpl(
    private val appDatabase: AppDatabase,
    private val credentialRepository: CredentialRepository,
    private val logger: Logger
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
                            id = entity.item_id,
                            fileName = entity.item_name,
                            bucketId = entity.bucket_id,
                            path = entity.path,
                            isFolder = entity.is_folder == 1L,
                            isBucket = entity.is_bucket == 1L,
                            starredAt = Instant.fromEpochMilliseconds(entity.starred_at)
                        )
                    }
                }
        }.catch { error ->
            // This flow feeds combine() in several ViewModels - a DB failure must not tear the
            // whole UI state down.
            logger.e(error) { "Failed to read starred items" }
            emit(emptyList())
        }
    }

    override suspend fun starItem(item: StarredItem) {
        val credentialId = credentialRepository.getLastUsedId() ?: return
        appDatabase.appDatabaseQueries.insertStarredItem(
            credential_id = credentialId,
            item_id = item.id,
            item_name = item.fileName,
            bucket_id = item.bucketId,
            path = item.path,
            is_folder = if (item.isFolder) 1L else 0L,
            is_bucket = if (item.isBucket) 1L else 0L,
            starred_at = item.starredAt.toEpochMilliseconds()
        )
    }

    override suspend fun unstarItem(id: String) {
        val credentialId = credentialRepository.getLastUsedId() ?: return
        appDatabase.appDatabaseQueries.deleteStarredItem(credentialId, id)
    }

    override suspend fun isItemStarred(id: String): Boolean {
        val credentialId = credentialRepository.getLastUsedId() ?: return false
        return appDatabase.appDatabaseQueries.isItemStarred(credentialId, id).executeAsOne() > 0
    }

    override suspend fun clearAllStarredItems() {
        val credentialId = credentialRepository.getLastUsedId() ?: return
        appDatabase.appDatabaseQueries.deleteAllStarredItems(credentialId)
    }
}
