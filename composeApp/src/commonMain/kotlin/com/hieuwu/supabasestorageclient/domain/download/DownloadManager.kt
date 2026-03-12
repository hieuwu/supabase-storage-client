package com.hieuwu.supabasestorageclient.domain.download

import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.DownloadRepository
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.util.FileWriter
import com.hieuwu.supabasestorageclient.util.PermissionManager
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write
import io.github.jan.supabase.storage.DownloadStatus as SupabaseDownloadStatus
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class DownloadManager(
    private val storageRepository: StorageRepository,
    private val fileWriter: FileWriter,
    private val permissionManager: PermissionManager,
    private val downloadRepository: DownloadRepository,
    private val credentialRepository: CredentialRepository,
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository
) {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    // Accumulate byte data across chunks
    private val byteBuffers = mutableMapOf<String, MutableList<ByteArray>>()
    private val buffersMutex = Mutex()
    private val dbWriteChannel = Channel<DownloadItem>(Channel.UNLIMITED)

    init {
        scope.launch {
            dbWriteChannel.consumeEach { item ->
                val lastUsedId = credentialRepository.getLastUsedId()
                if (lastUsedId != null) {
                    downloadRepository.insertDownloadItem(lastUsedId, item)
                }
            }
        }
        scope.launch {
            credentialRepository.getCredentials().collectLatest {
                val lastUsedId = credentialRepository.getLastUsedId()
                if (lastUsedId != null) {
                    downloadRepository.getDownloadItems(lastUsedId).collect { items ->
                        _downloads.value = items
                    }
                } else {
                    _downloads.value = emptyList()
                }
            }
        }
    }

    fun download(bucketId: String, path: String, fileName: String, platformFile: PlatformFile) {
        val id = "$bucketId:$path"
        // Don't start duplicate active downloads
        if (_downloads.value.any { it.id == id && it.status == DownloadStatus.Downloading }) return

        val item = DownloadItem(
            id = id,
            fileName = fileName,
            bucketId = bucketId,
            path = path,
            from = "$bucketId/$path",
            totalSize = 0,
            destinationPath = platformFile.path ?: "Unknown path"
        )

        updateAndPersistItem(item)

        val job = scope.launch {
            try {
                buffersMutex.withLock {
                    byteBuffers[id] = mutableListOf()
                }

                if (!permissionManager.requestStoragePermission()) {
                    val currentItem = _downloads.value.find { it.id == id }
                    currentItem?.copy(status = DownloadStatus.Error)?.let { updateAndPersistItem(it) }
                    return@launch
                }

                storageRepository.downloadFileAsFlow(bucketId, path).collect { status ->
                    when (status) {
                        is SupabaseDownloadStatus.Progress -> {
                            val currentItem = _downloads.value.find { it.id == id }
                            currentItem?.copy(
                                downloadedSize = status.totalBytesReceived,
                                totalSize = status.contentLength,
                                status = DownloadStatus.Downloading
                            )?.let { updateAndPersistItem(it) }
                        }
                        is SupabaseDownloadStatus.ByteData -> {
                            buffersMutex.withLock {
                                byteBuffers[id]?.add(status.data)
                            }
                            // Update downloadedSize based on accumulated data
                            val currentSize = buffersMutex.withLock {
                                byteBuffers[id]?.sumOf { chunk -> chunk.size.toLong() } ?: 0L
                            }
                            val currentItem = _downloads.value.find { it.id == id }
                            currentItem?.copy(downloadedSize = currentSize)?.let { updateAndPersistItem(it) }
                        }
                        SupabaseDownloadStatus.Success -> {
                        }
                    }
                }

                // Download flow completed normally - write the file
                val buffers = buffersMutex.withLock {
                    byteBuffers.remove(id) ?: mutableListOf()
                }
                
                if (buffers.isNotEmpty()) {
                    val totalSizeBytes = buffers.sumOf { it.size }
                    val allBytes = ByteArray(totalSizeBytes)
                    var offset = 0
                    for (buffer in buffers) {
                        buffer.copyInto(allBytes, offset)
                        offset += buffer.size
                    }

                    // Write directly using FileKit's standard method
                    try {
                        platformFile.write(allBytes)
                    } catch (e: Exception) {
                        // Fallback or error logging
                        e.printStackTrace()
                    }

                    val currentItem = _downloads.value.find { it.id == id }
                    currentItem?.copy(
                        status = DownloadStatus.Completed,
                        downloadedTime = Clock.System.now(),
                        downloadedSize = totalSizeBytes.toLong()
                    )?.let { updateAndPersistItem(it) }
                } else {
                    val currentItem = _downloads.value.find { it.id == id }
                    currentItem?.copy(status = DownloadStatus.Completed)?.let { updateAndPersistItem(it) }
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
                buffersMutex.withLock {
                    byteBuffers.remove(id)
                }
                val currentItem = _downloads.value.find { it.id == id }
                currentItem?.copy(status = DownloadStatus.Error)?.let { updateAndPersistItem(it) }
            }
        }
        downloadJobs[id] = job
    }

    private fun updateAndPersistItem(item: DownloadItem) {
        _downloads.update { list ->
            val existing = list.indexOfFirst { it.id == item.id }
            if (existing >= 0) list.toMutableList().also { it[existing] = item }
            else list + item
        }
        dbWriteChannel.trySend(item)
    }

    fun pause(id: String) {
        downloadJobs[id]?.cancel()
        scope.launch {
            buffersMutex.withLock {
                byteBuffers.remove(id)
            }
        }
        val currentItem = _downloads.value.find { it.id == id }
        currentItem?.copy(status = DownloadStatus.Paused)?.let { updateAndPersistItem(it) }
    }

    fun resume(id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        // Cannot resume easily with PlatformFile if we didn't save the reference,
        // For now, this might fail or need a fallback since we only have string destinationPath.
        // We might need to ask the user to pick again or use the old logic if destinationPath is valid.
        // We'll leave it as is for now but note that true 'resume' with a new picked file needs UI interaction.
    }

    fun cancel(id: String) {
        downloadJobs[id]?.cancel()
        scope.launch {
            buffersMutex.withLock {
                byteBuffers.remove(id)
            }
            val lastUsedId = credentialRepository.getLastUsedId()
            if (lastUsedId != null) {
                downloadRepository.deleteDownloadItem(lastUsedId, id)
            }
        }
        _downloads.update { it.filter { item -> item.id != id } }
    }

    fun deleteDownload(id: String) {
        cancel(id)
    }
}
