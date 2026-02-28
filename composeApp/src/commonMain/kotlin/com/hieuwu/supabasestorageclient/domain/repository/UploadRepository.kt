package com.hieuwu.supabasestorageclient.domain.repository

import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import kotlinx.coroutines.flow.Flow

interface UploadRepository {
    fun getUploadItems(credentialId: String): Flow<List<UploadItem>>
    suspend fun insertUploadItem(credentialId: String, item: UploadItem)
    suspend fun deleteUploadItem(credentialId: String, id: String)
    suspend fun deleteAllUploadItems(credentialId: String)
}
