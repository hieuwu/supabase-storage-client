package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.resumable.ResumableClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toInstant

import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive

class StorageRepositoryImpl(
    private val supabaseClientManager: SupabaseClientManager
) : StorageRepository {

    override suspend fun getBuckets(): List<Bucket> {
        val client = supabaseClientManager.client.first() ?: throw IllegalStateException("Supabase client not initialized")
        return client.storage.retrieveBuckets().map { bucket ->
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
        val client = supabaseClientManager.client.first() ?: throw IllegalStateException("Supabase client not initialized")
        val bucket = client.storage.from(bucketId)
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
        val client = supabaseClientManager.client.first() ?: throw IllegalStateException("Supabase client not initialized")
        return client.storage.from(bucketId).publicUrl(path)
    }

    override suspend fun downloadFile(bucketId: String, path: String): ByteArray {
        val client = supabaseClientManager.client.first() ?: throw IllegalStateException("Supabase client not initialized")
        return client.storage.from(bucketId).downloadPublic(path)
    }

    override suspend fun deleteFile(bucketId: String, path: String) {
        val client = supabaseClientManager.client.first() ?: throw IllegalStateException("Supabase client not initialized")
        client.storage.from(bucketId).delete(path)
    }

    override suspend fun getFileMetadata(bucketId: String, path: String): StorageItem {
        val client = supabaseClientManager.client.first() ?: throw IllegalStateException("Supabase client not initialized")
        val bucket = client.storage.from(bucketId)
        
        // Split path to get parent directory and filename
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
}

