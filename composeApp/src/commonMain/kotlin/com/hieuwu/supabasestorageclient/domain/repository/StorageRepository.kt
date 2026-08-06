package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.model.StorageDownloadStatus
import com.hieuwu.supabasestorageclient.domain.model.StorageUploadStatus
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    suspend fun getBuckets(): List<Bucket>
    suspend fun getBucketContents(bucketId: String, path: String): List<StorageItem>
    suspend fun getPublicUrl(bucketId: String, path: String): String
    suspend fun downloadFile(bucketId: String, path: String): ByteArray
    fun downloadFileAsFlow(bucketId: String, path: String): Flow<StorageDownloadStatus>
    suspend fun deleteFile(bucketId: String, path: String)
    suspend fun getFileMetadata(bucketId: String, path: String): StorageItem
    suspend fun moveFile(bucketId: String, fromPath: String, toPath: String)
    suspend fun createFolder(bucketId: String, path: String)
    fun uploadFileAsFlow(bucketId: String, path: String, data: ByteArray): Flow<StorageUploadStatus>
    suspend fun emptyBucket(bucketId: String)
    suspend fun deleteBucket(bucketId: String)
    suspend fun createBucket(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?)
    suspend fun updateBucket(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?)
    suspend fun clearCache(credentialId: String? = null)
    suspend fun clearBucketsCache()
    suspend fun clearContentsCache(bucketId: String)
}
