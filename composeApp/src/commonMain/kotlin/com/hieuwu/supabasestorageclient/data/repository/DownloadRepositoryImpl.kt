package com.hieuwu.supabasestorageclient.data.repository

import com.hieuwu.supabasestorageclient.database.AppDatabase
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.domain.repository.DownloadRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class DownloadRepositoryImpl(
    private val database: AppDatabase
) : DownloadRepository {

    override fun getDownloadItems(credentialId: String): Flow<List<DownloadItem>> {
        return database.appDatabaseQueries.getDownloadItems(credentialId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { entity ->
                    DownloadItem(
                        id = entity.id,
                        fileName = entity.file_name,
                        bucketId = entity.bucket_id,
                        path = entity.path,
                        totalSize = entity.total_size,
                        downloadedSize = entity.downloaded_size,
                        status = DownloadStatus.valueOf(entity.status),
                        downloadedTime = entity.downloaded_time?.let { Instant.parse(it) },
                        destinationPath = entity.destination_path
                    )
                }
            }
    }

    override suspend fun insertDownloadItem(credentialId: String, item: DownloadItem) {
        database.appDatabaseQueries.insertDownloadItem(
            id = item.id,
            credential_id = credentialId,
            file_name = item.fileName,
            bucket_id = item.bucketId,
            path = item.path,
            total_size = item.totalSize,
            downloaded_size = item.downloadedSize,
            status = item.status.name,
            downloaded_time = item.downloadedTime?.toString(),
            destination_path = item.destinationPath
        )
    }

    override suspend fun deleteDownloadItem(credentialId: String, id: String) {
        database.appDatabaseQueries.deleteDownloadItem(id, credentialId)
    }

    override suspend fun deleteAllDownloadItems(credentialId: String) {
        database.appDatabaseQueries.deleteAllDownloadItems(credentialId)
    }
}
