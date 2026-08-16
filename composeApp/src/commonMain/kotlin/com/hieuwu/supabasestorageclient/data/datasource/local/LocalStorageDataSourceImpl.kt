package com.hieuwu.supabasestorageclient.data.datasource.local

import com.hieuwu.supabasestorageclient.core.parseInstantOrNull
import com.hieuwu.supabasestorageclient.data.datasource.LocalStorageDataSource
import com.hieuwu.supabasestorageclient.database.AppDatabase
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem

class LocalStorageDataSourceImpl(
    private val database: AppDatabase
) : LocalStorageDataSource {

    private val dbQueries = database.appDatabaseQueries

    override suspend fun getBuckets(credentialId: String): List<Bucket> {
        return dbQueries.getAllBuckets(credentialId).executeAsList().map { bucket ->
            Bucket(
                id = bucket.id,
                name = bucket.name,
                owner = bucket.owner,
                public = bucket.is_public != 0L,
                createdAt = bucket.created_at,
                updatedAt = bucket.updated_at,
                allowedMimeTypes = bucket.allowed_mime_types?.split(","),
                fileSizeLimit = bucket.file_size_limit
            )
        }
    }

    override suspend fun saveBuckets(credentialId: String, buckets: List<Bucket>) {
        buckets.forEach { bucket ->
            dbQueries.insertBucket(
                credential_id = credentialId,
                id = bucket.id,
                name = bucket.name,
                owner = bucket.owner,
                is_public = if (bucket.public) 1L else 0L,
                created_at = bucket.createdAt,
                updated_at = bucket.updatedAt,
                allowed_mime_types = bucket.allowedMimeTypes?.joinToString(","),
                file_size_limit = bucket.fileSizeLimit
            )
        }
    }

    override suspend fun getStorageItems(
        credentialId: String,
        bucketId: String,
        path: String
    ): List<StorageItem> {
        val cached = if (path.isEmpty()) {
            dbQueries.getStorageItemsForBucketRoot(credentialId, bucketId).executeAsList()
        } else {
            dbQueries.getStorageItemsForBucketPath(credentialId, bucketId, "$path/%").executeAsList()
        }

        // Timestamps come back as strings written by a possibly older app version, so a malformed
        // value drops to null instead of failing the whole cache read.
        return cached.map { item ->
            StorageItem(
                name = item.name,
                id = item.id,
                updatedAt = parseInstantOrNull(item.updated_at, "cached ${item.name}.updatedAt"),
                createdAt = parseInstantOrNull(item.created_at, "cached ${item.name}.createdAt"),
                lastAccessedAt = parseInstantOrNull(item.last_accessed_at, "cached ${item.name}.lastAccessedAt"),
                metadata = emptyMap(),
                isFolder = item.is_folder != 0L,
                size = item.size
            )
        }
    }

    override suspend fun saveStorageItem(
        credentialId: String,
        bucketId: String,
        path: String,
        item: StorageItem
    ) {
        dbQueries.insertStorageItem(
            credential_id = credentialId,
            bucket_id = bucketId,
            path = if (path.isEmpty()) item.name else "$path/${item.name}",
            name = item.name,
            id = item.id,
            is_folder = if (item.isFolder) 1L else 0L,
            size = item.size,
            updated_at = item.updatedAt?.toString(),
            created_at = item.createdAt?.toString(),
            last_accessed_at = item.lastAccessedAt?.toString()
        )
    }

    override suspend fun clearCache(credentialId: String) {
        dbQueries.deleteAllBuckets(credentialId)
        dbQueries.deleteAllStorageItems(credentialId)
    }

    override suspend fun clearBucketsCache(credentialId: String) {
        dbQueries.deleteAllBuckets(credentialId)
    }

    override suspend fun clearContentsCache(credentialId: String, bucketId: String) {
        dbQueries.deleteStorageItemsForBucket(credentialId, bucketId)
    }
}
