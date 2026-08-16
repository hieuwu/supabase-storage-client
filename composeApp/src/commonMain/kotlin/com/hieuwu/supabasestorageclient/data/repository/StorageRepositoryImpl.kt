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

    /** Wraps a remote call, logging whatever it failed with before handing the failure back. */
    private suspend fun <T> remoteCall(description: String, block: suspend () -> T): Result<T> =
        runCatching { block() }.onFailure { error ->
            logger.e(error) { "Failed to $description" }
        }

    override suspend fun getBuckets(): Result<List<Bucket>> {
        val credentialId = getCurrentCredentialId()
        logger.d { "Fetching buckets for credential: $credentialId" }
        val remote = runCatching { remoteDataSource.getBuckets() }
        remote.onSuccess { remoteBuckets ->
            // A cache write failure must not fail a successful fetch.
            runCatching { localDataSource.saveBuckets(credentialId, remoteBuckets) }
                .onFailure { error -> logger.w(error) { "Failed to cache buckets for $credentialId" } }
            return remote
        }
        logger.e(remote.exceptionOrNull()) { "Error fetching buckets, trying cache" }
        val cached = runCatching { localDataSource.getBuckets(credentialId) }
            .getOrElse { cacheError ->
                logger.e(cacheError) { "Bucket cache read failed for $credentialId" }
                emptyList()
            }
        // With nothing cached there is nothing to show, so the original failure is the answer.
        return if (cached.isEmpty()) remote else Result.success(cached)
    }

    override suspend fun getBucketContents(bucketId: String, path: String): Result<List<StorageItem>> {
        val credentialId = getCurrentCredentialId()
        logger.d { "Fetching contents for bucket: $bucketId, path: $path, credential: $credentialId" }
        val remote = runCatching { remoteDataSource.getBucketContents(bucketId, path) }
        remote.onSuccess { items ->
            runCatching {
                items.forEach { item ->
                    localDataSource.saveStorageItem(credentialId, bucketId, path, item)
                }
            }.onFailure { error ->
                logger.w(error) { "Failed to cache contents of $bucketId at '$path'" }
            }
            return remote
        }
        logger.e(remote.exceptionOrNull()) { "Error fetching contents for $bucketId at '$path', trying cache" }
        val cached = runCatching { localDataSource.getStorageItems(credentialId, bucketId, path) }
            .getOrElse { cacheError ->
                logger.e(cacheError) { "Contents cache read failed for $bucketId at '$path'" }
                emptyList()
            }
        // With nothing cached there is nothing to show, so the original failure is the answer.
        return if (cached.isEmpty()) remote else Result.success(cached)
    }

    override suspend fun getPublicUrl(bucketId: String, path: String): Result<String> =
        remoteCall("get public url for $bucketId/$path") {
            remoteDataSource.getPublicUrl(bucketId, path)
        }

    override suspend fun downloadFile(bucketId: String, path: String): Result<ByteArray> =
        remoteCall("download $bucketId/$path") {
            remoteDataSource.downloadFile(bucketId, path)
        }

    override fun downloadFileAsFlow(bucketId: String, path: String): Flow<StorageDownloadStatus> {
        return remoteDataSource.downloadFileAsFlow(bucketId, path)
    }

    override suspend fun deleteFile(bucketId: String, path: String): Result<Unit> =
        remoteCall("delete $bucketId/$path") {
            remoteDataSource.deleteFile(bucketId, path)
        }

    override suspend fun getFileMetadata(bucketId: String, path: String): Result<StorageItem> {
        val name = if (path.contains("/")) path.substringAfterLast("/") else path
        return remoteCall("read metadata for $bucketId/$path") {
            remoteDataSource.getBucketContents(bucketId, path.substringBeforeLast("/", ""))
        }.fold(
            onSuccess = { items ->
                items.find { it.name == name }
                    ?.let { Result.success(it) }
                    ?: run {
                        logger.e { "File not found: $bucketId/$path" }
                        Result.failure(NoSuchElementException("File not found: $bucketId/$path"))
                    }
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun moveFile(bucketId: String, fromPath: String, toPath: String): Result<Unit> =
        remoteCall("move $bucketId/$fromPath to $toPath") {
            remoteDataSource.moveFile(bucketId, fromPath, toPath)
        }

    override suspend fun createFolder(bucketId: String, path: String): Result<Unit> =
        remoteCall("create folder $bucketId/$path") {
            remoteDataSource.createFolder(bucketId, path)
        }

    override fun uploadFileAsFlow(
        bucketId: String,
        path: String,
        data: ByteArray
    ): Flow<StorageUploadStatus> {
        return remoteDataSource.uploadFileAsFlow(bucketId, path, data)
    }

    override suspend fun emptyBucket(bucketId: String): Result<Unit> =
        remoteCall("empty bucket $bucketId") { remoteDataSource.emptyBucket(bucketId) }

    override suspend fun deleteBucket(bucketId: String): Result<Unit> =
        remoteCall("delete bucket $bucketId") { remoteDataSource.deleteBucket(bucketId) }

    override suspend fun createBucket(
        id: String,
        public: Boolean,
        fileSizeLimit: Long?,
        unit: SizeUnit?
    ): Result<Unit> = remoteCall("create bucket $id") {
        remoteDataSource.createBucket(id, public, fileSizeLimit, unit)
    }

    override suspend fun updateBucket(
        id: String,
        public: Boolean,
        fileSizeLimit: Long?,
        unit: SizeUnit?
    ): Result<Unit> = remoteCall("update bucket $id") {
        remoteDataSource.updateBucket(id, public, fileSizeLimit, unit)
    }

    override suspend fun clearCache(credentialId: String?): Result<Unit> =
        runCatching { localDataSource.clearCache(credentialId ?: getCurrentCredentialId()) }
            .onFailure { error -> logger.e(error) { "Failed to clear cache for $credentialId" } }

    override suspend fun clearBucketsCache(): Result<Unit> =
        runCatching { localDataSource.clearBucketsCache(getCurrentCredentialId()) }
            .onFailure { error -> logger.e(error) { "Failed to clear buckets cache" } }

    override suspend fun clearContentsCache(bucketId: String): Result<Unit> =
        runCatching { localDataSource.clearContentsCache(getCurrentCredentialId(), bucketId) }
            .onFailure { error -> logger.e(error) { "Failed to clear contents cache for $bucketId" } }
}
