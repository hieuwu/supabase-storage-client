package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.data.datasource.LocalStorageDataSource
import com.hieuwu.supabasestorageclient.data.datasource.RemoteStorageDataSource
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.StorageDownloadStatus
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.model.StorageUploadStatus
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow

class StorageRepositoryImpl(
    private val remoteDataSource: RemoteStorageDataSource,
    private val localDataSource: LocalStorageDataSource,
    private val credentialRepository: CredentialRepository,
    private val logger: Logger
) : StorageRepository {

    private fun getCurrentCredentialId(): String {
        return credentialRepository.getLastUsedId() ?: "default"
    }

    override suspend fun getBuckets(): List<Bucket> {
        val credentialId = getCurrentCredentialId()
        logger.d { "Fetching buckets for credential: $credentialId" }
        return try {
            val remoteBuckets = remoteDataSource.getBuckets()
            localDataSource.saveBuckets(credentialId, remoteBuckets)
            remoteBuckets
        } catch (e: Exception) {
            logger.e(e) { "Error fetching buckets, trying cache" }
            val cached = localDataSource.getBuckets(credentialId)
            if (cached.isEmpty()) throw e else cached
        }
    }

    override suspend fun getBucketContents(bucketId: String, path: String): List<StorageItem> {
        val credentialId = getCurrentCredentialId()
        logger.d { "Fetching contents for bucket: $bucketId, path: $path, credential: $credentialId" }
        return try {
            val items = remoteDataSource.getBucketContents(bucketId, path)
            items.forEach { item ->
                localDataSource.saveStorageItem(credentialId, bucketId, path, item)
            }
            items
        } catch (e: Exception) {
            logger.e(e) { "Error fetching bucket contents for $bucketId at $path, trying cache" }
            val cached = localDataSource.getStorageItems(credentialId, bucketId, path)
            if (cached.isEmpty()) throw e else cached
        }
    }

    override suspend fun getPublicUrl(bucketId: String, path: String): String {
        return remoteDataSource.getPublicUrl(bucketId, path)
    }

    override suspend fun downloadFile(bucketId: String, path: String): ByteArray {
        return remoteDataSource.downloadFile(bucketId, path)
    }

    override fun downloadFileAsFlow(bucketId: String, path: String): Flow<StorageDownloadStatus> {
        return remoteDataSource.downloadFileAsFlow(bucketId, path)
    }

    override suspend fun deleteFile(bucketId: String, path: String) {
        remoteDataSource.deleteFile(bucketId, path)
    }

    override suspend fun getFileMetadata(bucketId: String, path: String): StorageItem {
        // We can check local cache first if we want
        return remoteDataSource.getBucketContents(bucketId, path.substringBeforeLast("/", "")).find { 
            val name = if (path.contains("/")) path.substringAfterLast("/") else path
            it.name == name 
        } ?: throw Exception("File not found")
    }

    override suspend fun moveFile(bucketId: String, fromPath: String, toPath: String) {
        remoteDataSource.moveFile(bucketId, fromPath, toPath)
    }

    override suspend fun createFolder(bucketId: String, path: String) {
        remoteDataSource.createFolder(bucketId, path)
    }

    override fun uploadFileAsFlow(
        bucketId: String,
        path: String,
        data: ByteArray
    ): Flow<StorageUploadStatus> {
        return remoteDataSource.uploadFileAsFlow(bucketId, path, data)
    }

    override suspend fun emptyBucket(bucketId: String) {
        remoteDataSource.emptyBucket(bucketId)
    }

    override suspend fun deleteBucket(bucketId: String) {
        remoteDataSource.deleteBucket(bucketId)
    }

    override suspend fun createBucket(
        id: String,
        public: Boolean,
        fileSizeLimit: Long?,
        unit: SizeUnit?
    ) {
        remoteDataSource.createBucket(id, public, fileSizeLimit, unit)
    }

    override suspend fun clearCache(credentialId: String?) {
        val id = credentialId ?: getCurrentCredentialId()
        localDataSource.clearCache(id)
    }

    override suspend fun clearBucketsCache() {
        localDataSource.clearBucketsCache(getCurrentCredentialId())
    }

    override suspend fun clearContentsCache(bucketId: String) {
        localDataSource.clearContentsCache(getCurrentCredentialId(), bucketId)
    }
}