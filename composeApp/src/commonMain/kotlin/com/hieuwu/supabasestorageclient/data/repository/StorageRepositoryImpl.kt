package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import io.github.jan.supabase.storage.DownloadStatus
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.BucketApi
import io.github.jan.supabase.storage.downloadPublicAsFlow
import io.github.jan.supabase.storage.uploadAsFlow
import io.github.jan.supabase.storage.storage
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
                updatedAt = bucket.updatedAt.toString()
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
        client().storage.from(bucketId).move(fromPath, toPath)
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
}