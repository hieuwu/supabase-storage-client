package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloadItems(credentialId: String): Flow<List<DownloadItem>>
    suspend fun insertDownloadItem(credentialId: String, item: DownloadItem)
    suspend fun deleteDownloadItem(credentialId: String, id: String)
    suspend fun deleteAllDownloadItems(credentialId: String)
}
