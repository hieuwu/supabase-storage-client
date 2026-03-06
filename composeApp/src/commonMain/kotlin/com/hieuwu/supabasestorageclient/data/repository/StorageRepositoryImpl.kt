package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import io.github.jan.supabase.storage.DownloadStatus
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.downloadPublicAsFlow
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import co.touchlab.kermit.Logger
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class StorageRepositoryImpl(
    private val supabaseClientManager: SupabaseClientManager,
    private val logger: Logger,
    private val database: com.hieuwu.supabasestorageclient.database.AppDatabase,
    private val credentialRepository: com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
) : StorageRepository {

    private suspend fun client() =
        supabaseClientManager.client.first() ?: throw IllegalStateException("Supabase client not initialized")

    private val dbQueries = database.appDatabaseQueries

    private fun getCurrentCredentialId(): String {
        return credentialRepository.getLastUsedId() ?: "default"
    }

    override suspend fun getBuckets(): List<Bucket> {
        val credentialId = getCurrentCredentialId()
        logger.d { "Fetching buckets for credential: $credentialId" }
        return try {
            val cachedBuckets = dbQueries.getAllBuckets(credentialId).executeAsList()
            if (cachedBuckets.isNotEmpty()) {
                logger.d { "Retrieved ${cachedBuckets.size} buckets from local cache" }
            }
            
            val mappedCached = cachedBuckets.map { bucket ->
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
            
            // If we have cached data, we can return it and fetch in background? 
            // For now, let's just use it as a cache: try network, if fails use cache.
            // Or better: update cache always.
            
            logger.d { "Fetching buckets from remote Supabase" }
            val remoteBuckets = client().storage.retrieveBuckets().map { bucket ->
                Bucket(
                    id = bucket.id,
                    name = bucket.name,
                    owner = bucket.owner ?: "",
                    public = bucket.public,
                    createdAt = bucket.createdAt.toString(),
                    updatedAt = bucket.updatedAt.toString(),
                    allowedMimeTypes = bucket.allowedMimeTypes,
                    fileSizeLimit = bucket.fileSizeLimit
                )
            }

            // Update cache
            remoteBuckets.forEach { bucket ->
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
            
            remoteBuckets
        } catch (e: Exception) {
            logger.e(e) { "Error fetching buckets, trying cache" }
            val cached = dbQueries.getAllBuckets(credentialId).executeAsList().map { bucket ->
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
            if (cached.isEmpty()) throw e else cached
        }
    }

    override suspend fun getBucketContents(bucketId: String, path: String): List<StorageItem> {
        val credentialId = getCurrentCredentialId()
        logger.d { "Fetching contents for bucket: $bucketId, path: $path, credential: $credentialId" }
        return try {
            val bucket = client().storage.from(bucketId)
            logger.d { "Fetching contents from remote Supabase" }
            val list = bucket.list(path)
            val items = list.map { file ->
                val size = file.metadata?.get("size")?.jsonPrimitive?.longOrNull
                val item = StorageItem(
                    name = file.name,
                    id = file.id,
                    updatedAt = file.updatedAt,
                    createdAt = file.createdAt,
                    lastAccessedAt = file.lastAccessedAt,
                    metadata = emptyMap(),
                    isFolder = file.id == null,
                    size = size
                )
                
                // Update cache
                dbQueries.insertStorageItem(
                    credential_id = credentialId,
                    bucket_id = bucketId,
                    path = if (path.isEmpty()) file.name else "$path/${file.name}",
                    name = file.name,
                    id = file.id,
                    is_folder = if (file.id == null) 1L else 0L,
                    size = size,
                    updated_at = file.updatedAt?.toString(),
                    created_at = file.createdAt?.toString(),
                    last_accessed_at = file.lastAccessedAt?.toString()
                )
                item
            }
            items
        } catch (e: Exception) {
            logger.e(e) { "Error fetching bucket contents for bucket $bucketId at path $path, trying cache" }
            val cached = if (path.isEmpty()) {
                logger.d { "Fetching contents from local cache (root)" }
                dbQueries.getStorageItemsForBucketRoot(credentialId, bucketId).executeAsList()
            } else {
                logger.d { "Fetching contents from local cache (path: $path)" }
                dbQueries.getStorageItemsForBucketPath(credentialId, bucketId, "$path/%").executeAsList()
            }
            
            val mapped = cached.map { item ->
                StorageItem(
                    name = item.name,
                    id = item.id,
                    updatedAt = item.updated_at?.let { kotlinx.datetime.Instant.parse(it) },
                    createdAt = item.created_at?.let { kotlinx.datetime.Instant.parse(it) },
                    lastAccessedAt = item.last_accessed_at?.let { kotlinx.datetime.Instant.parse(it) },
                    metadata = emptyMap(),
                    isFolder = item.is_folder != 0L,
                    size = item.size
                )
            }
            if (mapped.isEmpty()) throw e else mapped
        }
    }

    override suspend fun getPublicUrl(bucketId: String, path: String): String {
        return client().storage.from(bucketId).publicUrl(path)
    }

    override suspend fun downloadFile(bucketId: String, path: String): ByteArray {
        return client().storage.from(bucketId).downloadPublic(path)
    }

    override fun downloadFileAsFlow(bucketId: String, path: String): Flow<DownloadStatus> = flow {
        val storage = client().storage.from(bucketId)
        emitAll(storage.downloadPublicAsFlow(path))
    }

    override suspend fun deleteFile(bucketId: String, path: String) {
        client().storage.from(bucketId).delete(path)
    }

    override suspend fun getFileMetadata(bucketId: String, path: String): StorageItem {
        val bucket = client().storage.from(bucketId)
        val lastSlash = path.lastIndexOf('/')
        val parentPath = if (lastSlash != -1) path.substring(0, lastSlash) else ""
        val fileName = if (lastSlash != -1) path.substring(lastSlash + 1) else path
        val list = bucket.list(parentPath)
        val file = list.find { it.name == fileName } ?: throw Exception("File not found")
        val size = file.metadata?.get("size")?.jsonPrimitive?.longOrNull
        return StorageItem(
            name = file.name,
            id = file.id,
            updatedAt = file.updatedAt,
            createdAt = file.createdAt,
            lastAccessedAt = file.lastAccessedAt,
            metadata = emptyMap(),
            isFolder = file.id == null,
            size = size
        )
    }

    override suspend fun moveFile(bucketId: String, fromPath: String, toPath: String) {
        val storage = client().storage.from(bucketId)
        try {
            storage.move(fromPath, toPath)
        } catch (e: Exception) {
            // If move fails, it might be a folder. Let's try recursive move.
            val items = storage.list(fromPath)
            if (items.isNotEmpty()) {
                moveFolderRecursive(bucketId, fromPath, toPath)
            } else {
                throw e
            }
        }
    }

    private suspend fun moveFolderRecursive(bucketId: String, fromPath: String, toPath: String) {
        val storage = client().storage.from(bucketId)
        val items = storage.list(fromPath)
        for (item in items) {
            val itemFromPath = "$fromPath/${item.name}"
            val itemToPath = "$toPath/${item.name}"
            if (item.id == null) { // Folder
                moveFolderRecursive(bucketId, itemFromPath, itemToPath)
            } else {
                storage.move(itemFromPath, itemToPath)
            }
        }
    }

    override suspend fun createFolder(bucketId: String, path: String) {
        val placeholderPath = if (path.endsWith("/")) "${path}.placeholder" else "$path/.placeholder"
        client().storage.from(bucketId).upload(placeholderPath, byteArrayOf(1))
    }

    override fun uploadFileAsFlow(
        bucketId: String,
        path: String,
        data: ByteArray
    ): Flow<UploadStatus> = flow {
        val bucket = client().storage.from(bucketId)
        emitAll(bucket.uploadAsFlow(path, data))
    }

    override suspend fun emptyBucket(bucketId: String) {
        client().storage.emptyBucket(bucketId)
    }

    override suspend fun deleteBucket(bucketId: String) {
        try {
            client().storage.deleteBucket(bucketId)
        } catch (e: Exception) {
            logger.e(e) { "Error deleting bucket $bucketId" }
            throw e
        }
    }

    override suspend fun createBucket(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?) {
        try {
            client().storage.createBucket(id) {
                this.public = public
                this.fileSizeLimit = when (unit) {
                    SizeUnit.BYTES -> fileSizeLimit?.bytes
                    SizeUnit.KILOBYTES -> fileSizeLimit?.kilobytes
                    SizeUnit.MEGABYTES -> fileSizeLimit?.megabytes
                    SizeUnit.GIGABYTES -> fileSizeLimit?.gigabytes
                    null -> null
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error creating bucket $id" }
            throw e
        }
    }

    override suspend fun clearCache(credentialId: String?) {
        val id = credentialId ?: getCurrentCredentialId()
        logger.d { "Clearing local cache for credential: $id" }
        dbQueries.deleteAllBuckets(id)
        dbQueries.deleteAllStorageItems(id)
    }

    override suspend fun clearBucketsCache() {
        val credentialId = getCurrentCredentialId()
        logger.d { "Clearing buckets cache for credential: $credentialId" }
        dbQueries.deleteAllBuckets(credentialId)
    }

    override suspend fun clearContentsCache(bucketId: String) {
        val credentialId = getCurrentCredentialId()
        logger.d { "Clearing contents cache for bucket: $bucketId, credential: $credentialId" }
        dbQueries.deleteStorageItemsForBucket(credentialId, bucketId)
    }
}