package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.database.AppDatabase
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.domain.repository.DownloadRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.core.toKxInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

class DownloadRepositoryImpl(
    private val database: AppDatabase,
    private val logger: Logger
) : DownloadRepository {

    override fun getDownloadItems(credentialId: String): Flow<List<DownloadItem>> {
        return database.appDatabaseQueries.getDownloadItems(credentialId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                // Status and timestamp are stored as free-form strings; a row written by an older
                // version must not take down the collector, so each field degrades on its own.
                entities.map { entity ->
                    DownloadItem(
                        id = entity.id,
                        fileName = entity.file_name,
                        bucketId = entity.bucket_id,
                        path = entity.path,
                        from = entity.from_path,
                        totalSize = entity.total_size,
                        downloadedSize = entity.downloaded_size,
                        status = runCatching { DownloadStatus.valueOf(entity.status) }
                            .getOrElse { error ->
                                logger.w(error) { "Unknown download status '${entity.status}' for ${entity.id}" }
                                DownloadStatus.Error
                            },
                        downloadedTime = entity.downloaded_time?.let { raw ->
                            runCatching { Instant.parse(raw).toKxInstant() }.getOrElse { error ->
                                logger.w(error) { "Invalid download time '$raw' for ${entity.id}" }
                                null
                            }
                        },
                        destinationPath = entity.destination_path,
                        sourcePath = entity.source_path,
                        errorMessage = entity.error_message
                    )
                }
            }
            .catch { error ->
                logger.e(error) { "Failed to read download items for $credentialId" }
                emit(emptyList())
            }
    }

    override suspend fun insertDownloadItem(credentialId: String, item: DownloadItem) {
        database.appDatabaseQueries.insertDownloadItem(
            id = item.id,
            credential_id = credentialId,
            file_name = item.fileName,
            bucket_id = item.bucketId,
            path = item.path,
            from_path = item.from,
            total_size = item.totalSize,
            downloaded_size = item.downloadedSize,
            status = item.status.name,
            downloaded_time = item.downloadedTime?.toString(),
            destination_path = item.destinationPath,
            source_path = item.sourcePath,
            error_message = item.errorMessage
        )
    }

    override suspend fun deleteDownloadItem(credentialId: String, id: String) {
        database.appDatabaseQueries.deleteDownloadItem(id, credentialId)
    }

    override suspend fun deleteAllDownloadItems(credentialId: String) {
        database.appDatabaseQueries.deleteAllDownloadItems(credentialId)
    }
}
