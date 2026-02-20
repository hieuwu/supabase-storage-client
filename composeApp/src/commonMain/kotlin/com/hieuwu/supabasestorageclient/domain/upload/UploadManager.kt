package com.hieuwu.supabasestorageclient.domain.upload

import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.model.UploadStatus
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.util.PermissionManager
import io.github.jan.supabase.storage.UploadStatus as SupabaseUploadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class UploadManager(
    private val storageRepository: StorageRepository,
    private val permissionManager: PermissionManager
) {
    private val _uploads = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploads: StateFlow<List<UploadItem>> = _uploads.asStateFlow()

    private val uploadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun upload(bucketId: String, path: String, fileName: String, data: ByteArray) {
        val id = "$bucketId:$path"
        if (_uploads.value.any { it.id == id && it.status == UploadStatus.Uploading }) return

        val item = UploadItem(
            id = id,
            fileName = fileName,
            bucketId = bucketId,
            path = path,
            totalSize = data.size.toLong()
        )

        _uploads.update { list ->
            val existing = list.indexOfFirst { it.id == id }
            if (existing >= 0) list.toMutableList().also { it[existing] = item }
            else list + item
        }

        val job = scope.launch {
            try {
                if (!permissionManager.requestStoragePermission()) {
                    updateStatus(id, UploadStatus.Error)
                    return@launch
                }

                storageRepository.uploadFileAsFlow(bucketId, path, data).collect { status ->
                    when (status) {
                        is SupabaseUploadStatus.Progress -> {
                            _uploads.update { list ->
                                list.map {
                                    if (it.id == id) it.copy(
                                        uploadedSize = status.totalBytesSend,
                                        totalSize = status.contentLength,
                                        status = UploadStatus.Uploading
                                    ) else it
                                }
                            }
                        }
                        is SupabaseUploadStatus.Success -> {
                            _uploads.update { list ->
                                list.map {
                                    if (it.id == id) it.copy(
                                        status = UploadStatus.Completed,
                                        uploadedTime = Clock.System.now(),
                                        uploadedSize = it.totalSize
                                    ) else it
                                }
                            }
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                updateStatus(id, UploadStatus.Error)
            }
        }
        uploadJobs[id] = job
    }

    private fun updateStatus(id: String, status: UploadStatus) {
        _uploads.update { list ->
            list.map { if (it.id == id) it.copy(status = status) else it }
        }
    }

    fun cancel(id: String) {
        uploadJobs[id]?.cancel()
        _uploads.update { it.filter { item -> item.id != id } }
    }
}
