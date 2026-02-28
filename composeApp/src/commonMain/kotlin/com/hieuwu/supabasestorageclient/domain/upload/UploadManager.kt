package com.hieuwu.supabasestorageclient.domain.upload

import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.model.UploadStatus
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.repository.UploadRepository
import com.hieuwu.supabasestorageclient.util.PermissionManager
import io.github.jan.supabase.storage.UploadStatus as SupabaseUploadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class UploadManager(
    private val storageRepository: StorageRepository,
    private val permissionManager: PermissionManager,
    private val uploadRepository: UploadRepository,
    private val credentialRepository: CredentialRepository
) {
    private val _uploads = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploads: StateFlow<List<UploadItem>> = _uploads.asStateFlow()

    private val uploadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            credentialRepository.getCredentials().collectLatest {
                val lastUsedId = credentialRepository.getLastUsedId()
                if (lastUsedId != null) {
                    uploadRepository.getUploadItems(lastUsedId).collect { items ->
                        _uploads.value = items
                    }
                } else {
                    _uploads.value = emptyList()
                }
            }
        }
    }

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

        updateAndPersistItem(item)

        val job = scope.launch {
            try {
                if (!permissionManager.requestStoragePermission()) {
                    val currentItem = _uploads.value.find { it.id == id }
                    currentItem?.copy(status = UploadStatus.Error)?.let { updateAndPersistItem(it) }
                    return@launch
                }

                storageRepository.uploadFileAsFlow(bucketId, path, data).collect { status ->
                    when (status) {
                        is SupabaseUploadStatus.Progress -> {
                            val currentItem = _uploads.value.find { it.id == id }
                            currentItem?.copy(
                                uploadedSize = status.totalBytesSend,
                                totalSize = status.contentLength,
                                status = UploadStatus.Uploading
                            )?.let { updateAndPersistItem(it) }
                        }
                        is SupabaseUploadStatus.Success -> {
                            val currentItem = _uploads.value.find { it.id == id }
                            currentItem?.copy(
                                status = UploadStatus.Completed,
                                uploadedTime = Clock.System.now(),
                                uploadedSize = currentItem.totalSize
                            )?.let { updateAndPersistItem(it) }
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                val currentItem = _uploads.value.find { it.id == id }
                currentItem?.copy(status = UploadStatus.Error)?.let { updateAndPersistItem(it) }
            }
        }
        uploadJobs[id] = job
    }

    private fun updateAndPersistItem(item: UploadItem) {
        _uploads.update { list ->
            val existing = list.indexOfFirst { it.id == item.id }
            if (existing >= 0) list.toMutableList().also { it[existing] = item }
            else list + item
        }
        scope.launch {
            val lastUsedId = credentialRepository.getLastUsedId()
            if (lastUsedId != null) {
                uploadRepository.insertUploadItem(lastUsedId, item)
            }
        }
    }

    fun cancel(id: String) {
        uploadJobs[id]?.cancel()
        scope.launch {
            val lastUsedId = credentialRepository.getLastUsedId()
            if (lastUsedId != null) {
                uploadRepository.deleteUploadItem(lastUsedId, id)
            }
        }
        _uploads.update { it.filter { item -> item.id != id } }
    }
}
