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
}

