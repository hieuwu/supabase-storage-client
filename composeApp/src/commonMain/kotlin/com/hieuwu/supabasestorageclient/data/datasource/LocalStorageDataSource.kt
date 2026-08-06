package com.hieuwu.supabasestorageclient.data.datasource

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem

interface LocalStorageDataSource {
    suspend fun getBuckets(credentialId: String): List<Bucket>
    suspend fun saveBuckets(credentialId: String, buckets: List<Bucket>)
    suspend fun getStorageItems(credentialId: String, bucketId: String, path: String): List<StorageItem>
    suspend fun saveStorageItem(credentialId: String, bucketId: String, path: String, item: StorageItem)
    suspend fun clearCache(credentialId: String)
    suspend fun clearBucketsCache(credentialId: String)
    suspend fun clearContentsCache(credentialId: String, bucketId: String)
}