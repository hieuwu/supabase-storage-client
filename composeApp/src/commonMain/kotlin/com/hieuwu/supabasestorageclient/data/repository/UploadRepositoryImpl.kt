package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.database.AppDatabase
import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.model.UploadStatus
import com.hieuwu.supabasestorageclient.domain.repository.UploadRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class UploadRepositoryImpl(
    private val database: AppDatabase
) : UploadRepository {

    override fun getUploadItems(credentialId: String): Flow<List<UploadItem>> {
        return database.appDatabaseQueries.getUploadItems(credentialId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { entity ->
                    UploadItem(
                        id = entity.id,
                        fileName = entity.file_name,
                        bucketId = entity.bucket_id,
                        path = entity.path,
                        totalSize = entity.total_size,
                        uploadedSize = entity.uploaded_size,
                        status = UploadStatus.valueOf(entity.status),
                        uploadedTime = entity.uploaded_time?.let { Instant.parse(it) },
                        from = entity.from_path,
                        to = entity.to_path ?: "",
                        errorMessage = entity.error_message
                    )
                }
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
