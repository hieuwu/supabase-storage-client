package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem

interface StorageRepository {
    suspend fun getBuckets(): List<Bucket>
    suspend fun getBucketContents(bucketId: String, path: String): List<StorageItem>
}
