package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.SupabaseClientManager
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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class StorageRepositoryImpl(
    private val supabaseClientManager: SupabaseClientManager
) : StorageRepository {

    private suspend fun client() =
        supabaseClientManager.client.first() ?: throw IllegalStateException("Supabase client not initialized")

    override suspend fun getBuckets(): List<Bucket> {
        return client().storage.retrieveBuckets().map { bucket ->
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
    }

    override suspend fun getBucketContents(bucketId: String, path: String): List<StorageItem> {
        val bucket = client().storage.from(bucketId)
        val list = bucket.list(path)
        return list.map { file ->
            val size = file.metadata?.get("size")?.jsonPrimitive?.longOrNull
            StorageItem(
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
        client().storage.deleteBucket(bucketId)
    }

    override suspend fun createBucket(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?) {
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
    }
}