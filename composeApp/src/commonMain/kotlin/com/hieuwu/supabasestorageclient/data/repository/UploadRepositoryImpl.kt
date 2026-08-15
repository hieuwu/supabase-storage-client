package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.database.AppDatabase
import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.model.UploadStatus
import com.hieuwu.supabasestorageclient.domain.repository.UploadRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.core.parseInstantOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class UploadRepositoryImpl(
    private val database: AppDatabase,
    private val logger: Logger
) : UploadRepository {

    override fun getUploadItems(credentialId: String): Flow<List<UploadItem>> {
        return database.appDatabaseQueries.getUploadItems(credentialId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                // Status and timestamp are stored as free-form strings; a row written by an older
                // version must not take down the collector, so each field degrades on its own.
                entities.map { entity ->
                    UploadItem(
                        id = entity.id,
                        fileName = entity.file_name,
                        bucketId = entity.bucket_id,
                        path = entity.path,
                        totalSize = entity.total_size,
                        uploadedSize = entity.uploaded_size,
                        status = runCatching { UploadStatus.valueOf(entity.status) }
                            .getOrElse { error ->
                                logger.w(error) { "Unknown upload status '${entity.status}' for ${entity.id}" }
                                UploadStatus.Error
                            },
                        uploadedTime = parseInstantOrNull(entity.uploaded_time, "upload ${entity.id}"),
                        from = entity.from_path,
                        to = entity.to_path ?: "",
                        errorMessage = entity.error_message
                    )
                }
            }
            .catch { error ->
                logger.e(error) { "Failed to read upload items for $credentialId" }
                emit(emptyList())
            }
    }

    override suspend fun insertUploadItem(credentialId: String, item: UploadItem) {
        database.appDatabaseQueries.insertUploadItem(
            id = item.id,
            credential_id = credentialId,
            file_name = item.fileName,
            bucket_id = item.bucketId,
            path = item.path,
            total_size = item.totalSize,
            uploaded_size = item.uploadedSize,
            status = item.status.name,
            uploaded_time = item.uploadedTime?.toString(),
            from_path = item.from,
            to_path = item.to,
            error_message = item.errorMessage
        )
    }

    override suspend fun deleteUploadItem(credentialId: String, id: String) {
        database.appDatabaseQueries.deleteUploadItem(id, credentialId)
    }

    override suspend fun deleteAllUploadItems(credentialId: String) {
        database.appDatabaseQueries.deleteAllUploadItems(credentialId)
    }
}
