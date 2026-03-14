package com.hieuwu.supabasestorageclient.domain.upload

import com.hieuwu.supabasestorageclient.domain.model.UploadItem
import com.hieuwu.supabasestorageclient.domain.model.UploadStatus
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.repository.UploadRepository
import io.github.jan.supabase.storage.UploadStatus as SupabaseUploadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class UploadManager(
    private val storageRepository: StorageRepository,
    private val uploadRepository: UploadRepository,
    private val credentialRepository: CredentialRepository,
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository
) {
    private val _uploads = MutableStateFlow<List<UploadItem>>(emptyList())
    val uploads: StateFlow<List<UploadItem>> = _uploads.asStateFlow()

    private val uploadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            credentialRepository.lastUsedId.collectLatest { lastUsedId ->
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
        val lastUsedId = credentialRepository.getLastUsedId() ?: return
        val id = "$bucketId:$path"
        if (_uploads.value.any { it.id == id && it.status == UploadStatus.Uploading }) return

        val item = UploadItem(
            id = id,
            fileName = fileName,
            bucketId = bucketId,
            path = path,
            totalSize = data.size.toLong(),
            from = fileName,
            to = "$bucketId/$path"
        )

        updateAndPersistItem(lastUsedId, item)

        val job = scope.launch {
            try {

                storageRepository.uploadFileAsFlow(bucketId, path, data).collect { status ->
                    when (status) {
                        is SupabaseUploadStatus.Progress -> {
                            val currentItem = _uploads.value.find { it.id == id }
                            currentItem?.copy(
                                uploadedSize = status.totalBytesSend,
                                totalSize = status.contentLength,
                                status = UploadStatus.Uploading
                            )?.let { updateAndPersistItem(lastUsedId, it) }
                        }
                        is SupabaseUploadStatus.Success -> {
                            val currentItem = _uploads.value.find { it.id == id }
                            currentItem?.copy(
                                status = UploadStatus.Completed,
                                uploadedTime = Clock.System.now(),
                                uploadedSize = currentItem.totalSize
                            )?.let { updateAndPersistItem(lastUsedId, it) }
                        }

                        else -> {}
                    }
                }

                val currentItemAfter = _uploads.value.find { it.id == id }
                if (currentItemAfter?.status == UploadStatus.Uploading) {
                    currentItemAfter.copy(
                        status = UploadStatus.Completed,
                        uploadedTime = Clock.System.now(),
                        uploadedSize = currentItemAfter.totalSize
                    ).let { updateAndPersistItem(lastUsedId, it) }
                }

                // Check if this is the first operation
                scope.launch {
                    val settings = settingsRepository.getSettings().firstOrNull()
                    if (settings != null && !settings.isFirstOperationCompleted) {
                        settingsRepository.updateSettings(settings.copy(isFirstOperationCompleted = true))
                        purchaseRepository.triggerPaywall()
                    }
                }
            } catch (e: Exception) {
                val currentItem = _uploads.value.find { it.id == id }
                currentItem?.copy(status = UploadStatus.Error)?.let { updateAndPersistItem(lastUsedId, it) }
            }
        }
        uploadJobs[id] = job
    }

    private fun updateAndPersistItem(credentialId: String, item: UploadItem) {
        val currentLastUsedId = credentialRepository.getLastUsedId()
        if (credentialId == currentLastUsedId) {
            _uploads.update { list ->
                val existing = list.indexOfFirst { it.id == item.id }
                if (existing >= 0) list.toMutableList().also { it[existing] = item }
                else list + item
            }
        }
        scope.launch {
            uploadRepository.insertUploadItem(credentialId, item)
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
