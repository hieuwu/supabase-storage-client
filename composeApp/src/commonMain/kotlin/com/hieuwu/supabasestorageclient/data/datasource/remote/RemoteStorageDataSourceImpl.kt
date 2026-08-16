package com.hieuwu.supabasestorageclient.data.datasource.remote

import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.core.toKxInstant
import com.hieuwu.supabasestorageclient.data.datasource.RemoteStorageDataSource
import com.hieuwu.supabasestorageclient.data.network.SupabaseClientManager
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.SizeUnit
import com.hieuwu.supabasestorageclient.domain.model.StorageDownloadStatus
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.model.StorageUploadStatus
import io.github.jan.supabase.storage.DownloadStatus as SupabaseDownloadStatus
import io.github.jan.supabase.storage.UploadStatus as SupabaseUploadStatus
import io.github.jan.supabase.storage.downloadPublicAsFlow
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class RemoteStorageDataSourceImpl(
    private val supabaseClientManager: SupabaseClientManager,
    private val logger: Logger
) : RemoteStorageDataSource {

    private suspend fun client() =
        supabaseClientManager.client.first() ?: run {
            logger.e { "Supabase client is not initialized - no credential has been selected" }
            throw IllegalStateException("Supabase client not initialized")
        }

    override suspend fun getBuckets(): List<Bucket> {
        return client().storage.listBuckets().map { bucket ->
            Bucket(
                id = bucket.id,
                name = bucket.name,
                owner = bucket.owner,
                public = bucket.public,
                createdAt = bucket.createdAt.toString(),
                updatedAt = bucket.updatedAt.toString(),
                allowedMimeTypes = bucket.allowedMimeTypes,
                fileSizeLimit = bucket.fileSizeLimit
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
                updatedAt = file.updatedAt?.toKxInstant(),
                createdAt = file.createdAt?.toKxInstant(),
                lastAccessedAt = file.lastAccessedAt?.toKxInstant(),
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

    override fun downloadFileAsFlow(bucketId: String, path: String): Flow<StorageDownloadStatus> = flow {
        val storage = client().storage.from(bucketId)
        emitAll(storage.downloadPublicAsFlow(path).map { status ->
            when (status) {
                is SupabaseDownloadStatus.Progress -> StorageDownloadStatus.Progress(
                    status.totalBytesReceived,
                    status.contentLength
                )
                is SupabaseDownloadStatus.ByteData -> StorageDownloadStatus.ByteData(status.data)
                SupabaseDownloadStatus.Success -> StorageDownloadStatus.Success
            }
        })
    }

    override suspend fun deleteFile(bucketId: String, path: String) {
        client().storage.from(bucketId).delete(path)
    }

    override suspend fun moveFile(bucketId: String, fromPath: String, toPath: String) {
        val storage = client().storage.from(bucketId)
        val isFolder = runCatching { storage.list(fromPath).isNotEmpty() }
            .getOrElse { error ->
                logger.w(error) { "Could not list $bucketId/$fromPath, moving it as a single file" }
                false
            }
        if (isFolder) {
            logger.d { "Moving folder $bucketId/$fromPath to $toPath entry by entry" }
            moveFolderRecursive(bucketId, fromPath, toPath)
        } else {
            storage.move(fromPath, toPath)
        }
    }

    private suspend fun moveFolderRecursive(bucketId: String, fromPath: String, toPath: String) {
        val storage = client().storage.from(bucketId)
        val items = storage.list(fromPath)
        for (item in items) {
            val itemFromPath = "$fromPath/${item.name}"
            val itemToPath = "$toPath/${item.name}"
            if (item.id == null) {
                moveFolderRecursive(bucketId, itemFromPath, itemToPath)
            } else {
                storage.move(itemFromPath, itemToPath)
            }
        }
    }

    override suspend fun createFolder(bucketId: String, path: String) {
        val placeholderPath = if (path.endsWith("/")) "${path}.placeholder" else "$path/.placeholder"
        client().storage.from(bucketId).upload(placeholderPath, byteArrayOf(1))
    }

    override fun uploadFileAsFlow(
        bucketId: String,
        path: String,
        data: ByteArray
    ): Flow<StorageUploadStatus> = flow {
        val bucket = client().storage.from(bucketId)
        emitAll(bucket.uploadAsFlow(path, data).map { status ->
            when (status) {
                is SupabaseUploadStatus.Progress -> StorageUploadStatus.Progress(
                    status.totalBytesSend,
                    status.contentLength
                )
                is SupabaseUploadStatus.Success -> StorageUploadStatus.Success
            }
        })
    }

    override suspend fun emptyBucket(bucketId: String) {
        client().storage.emptyBucket(bucketId)
    }

    override suspend fun deleteBucket(bucketId: String) {
        client().storage.deleteBucket(bucketId)
    }

    override suspend fun createBucket(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?) {
        client().storage.createBucket(id) {
            this.public = public
            this.fileSizeLimit = when (unit) {
                SizeUnit.BYTES -> fileSizeLimit?.bytes
                SizeUnit.KILOBYTES -> fileSizeLimit?.kilobytes
                SizeUnit.MEGABYTES -> fileSizeLimit?.megabytes
                SizeUnit.GIGABYTES -> fileSizeLimit?.gigabytes
                null -> null
            }
        }
    }

    override suspend fun updateBucket(id: String, public: Boolean, fileSizeLimit: Long?, unit: SizeUnit?) {
        client().storage.updateBucket(id) {
            this.public = public
            this.fileSizeLimit = when (unit) {
                SizeUnit.BYTES -> fileSizeLimit?.bytes
                SizeUnit.KILOBYTES -> fileSizeLimit?.kilobytes
                SizeUnit.MEGABYTES -> fileSizeLimit?.megabytes
                SizeUnit.GIGABYTES -> fileSizeLimit?.gigabytes
                null -> null
            }
        }
    }
}
