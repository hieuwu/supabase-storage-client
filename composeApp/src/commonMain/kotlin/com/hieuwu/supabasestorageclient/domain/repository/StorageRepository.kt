package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import io.github.jan.supabase.storage.DownloadStatus
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    suspend fun getBuckets(): List<Bucket>
    suspend fun getBucketContents(bucketId: String, path: String): List<StorageItem>
    suspend fun getPublicUrl(bucketId: String, path: String): String
    suspend fun downloadFile(bucketId: String, path: String): ByteArray
    fun downloadFileAsFlow(bucketId: String, path: String): Flow<DownloadStatus>
    suspend fun deleteFile(bucketId: String, path: String)
    suspend fun getFileMetadata(bucketId: String, path: String): StorageItem
    suspend fun moveFile(bucketId: String, fromPath: String, toPath: String)
}
