package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.model.StorageDownloadStatus
import com.hieuwu.supabasestorageclient.domain.model.StorageUploadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Every suspending operation reports failure as a [Result] rather than by throwing, so callers
 * never have to catch-and-rethrow to pass an error up. The two streaming operations keep their
 * `Flow` shape - their failures surface at the collection site.
 */
interface StorageRepository {
    suspend fun getBuckets(): Result<List<Bucket>>
    suspend fun getBucketContents(bucketId: String, path: String): Result<List<StorageItem>>
    suspend fun getPublicUrl(bucketId: String, path: String): Result<String>
    suspend fun downloadFile(bucketId: String, path: String): Result<ByteArray>
    fun downloadFileAsFlow(bucketId: String, path: String): Flow<StorageDownloadStatus>
    suspend fun deleteFile(bucketId: String, path: String): Result<Unit>
    suspend fun getFileMetadata(bucketId: String, path: String): Result<StorageItem>
    suspend fun moveFile(bucketId: String, fromPath: String, toPath: String): Result<Unit>
    suspend fun createFolder(bucketId: String, path: String): Result<Unit>
    fun uploadFileAsFlow(bucketId: String, path: String, data: ByteArray): Flow<StorageUploadStatus>
    suspend fun emptyBucket(bucketId: String): Result<Unit>
    suspend fun deleteBucket(bucketId: String): Result<Unit>
    suspend fun createBucket(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?): Result<Unit>
    suspend fun updateBucket(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?): Result<Unit>
    suspend fun clearCache(credentialId: String? = null): Result<Unit>
    suspend fun clearBucketsCache(): Result<Unit>
    suspend fun clearContentsCache(bucketId: String): Result<Unit>
}
