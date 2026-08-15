package com.hieuwu.supabasestorageclient.domain.download

import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.error.StorageOperation
import com.hieuwu.supabasestorageclient.domain.error.storageErrorMessage
import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.domain.model.StorageDownloadStatus
import com.hieuwu.supabasestorageclient.domain.repository.CredentialRepository
import com.hieuwu.supabasestorageclient.domain.repository.DownloadRepository
import com.hieuwu.supabasestorageclient.domain.repository.PurchaseRepository
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class DownloadManager(
    private val storageRepository: StorageRepository,
    private val downloadRepository: DownloadRepository,
    private val credentialRepository: CredentialRepository,
    private val purchaseRepository: PurchaseRepository,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger
) {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    // Accumulate byte data across chunks
    private val byteBuffers = mutableMapOf<String, MutableList<ByteArray>>()
    private val buffersMutex = Mutex()
    private val dbWriteChannel = Channel<DownloadWriteTask>(Channel.UNLIMITED)

    private data class DownloadWriteTask(val credentialId: String, val item: DownloadItem)

    init {
        scope.launch {
            dbWriteChannel.consumeEach { task ->
                downloadRepository.insertDownloadItem(task.credentialId, task.item)
            }
        }
        scope.launch {
            credentialRepository.lastUsedId.collectLatest { lastUsedId ->
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
        val lastUsedId = credentialRepository.getLastUsedId() ?: return
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
            destinationPath = platformFile.path ?: "Unknown path",
            sourcePath = path.substringBeforeLast("/", "")
        )

        updateAndPersistItem(lastUsedId, item)

        val job = scope.launch {
            try {
                buffersMutex.withLock {
                    byteBuffers[id] = mutableListOf()
                }

                storageRepository.downloadFileAsFlow(bucketId, path).collect { status ->
                    when (status) {
                        is StorageDownloadStatus.Progress -> {
                            val currentItem = _downloads.value.find { it.id == id }
                            currentItem?.copy(
                                downloadedSize = status.totalBytesReceived,
                                totalSize = status.contentLength,
                                status = DownloadStatus.Downloading
                            )?.let { updateAndPersistItem(lastUsedId, it) }
                        }
                        is StorageDownloadStatus.ByteData -> {
                            buffersMutex.withLock {
                                byteBuffers[id]?.add(status.data)
                            }
                            // Update downloadedSize based on accumulated data
                            val currentSize = buffersMutex.withLock {
                                byteBuffers[id]?.sumOf { chunk -> chunk.size.toLong() } ?: 0L
                            }
                            val currentItem = _downloads.value.find { it.id == id }
                            currentItem?.copy(downloadedSize = currentSize)?.let { updateAndPersistItem(lastUsedId, it) }
                        }
                        StorageDownloadStatus.Success -> {
                        }
                    }
                }

                // Download flow completed normally - write the file
                val buffers = buffersMutex.withLock {
                    byteBuffers.remove(id) ?: mutableListOf()
                }
                
                val allBytes = concatenate(buffers)
                // A write failure must fail the download - it used to be swallowed and the item
                // was still marked as Completed even though nothing landed on disk.
                platformFile.write(allBytes)

                val currentItem = _downloads.value.find { it.id == id }
                currentItem?.copy(
                    status = DownloadStatus.Completed,
                    downloadedTime = Clock.System.now(),
                    downloadedSize = allBytes.size.toLong(),
                    errorMessage = null
                )?.let { updateAndPersistItem(lastUsedId, it) }
            } catch (e: CancellationException) {
                // pause()/cancel() clear the buffer for us - suspending here would only fail again
                throw e
            } catch (e: Exception) {
                buffersMutex.withLock {
                    byteBuffers.remove(id)
                }
                failDownload(lastUsedId, id, bucketId, path, e)
            }
        }
        downloadJobs[id] = job
    }

    fun downloadToDirectoryPath(bucketId: String, path: String, fileName: String, destinationDirectory: String) {
        val lastUsedId = credentialRepository.getLastUsedId() ?: return
        // The picked directory is not always a file system path - on Android it is a Storage
        // Access Framework uri - so let FileKit resolve the child file instead of concatenating.
        val destinationFile = PlatformFile(PlatformFile(destinationDirectory), fileName)
        val destinationPath = destinationFile.path
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
            destinationPath = destinationPath,
            sourcePath = path.substringBeforeLast("/", "")
        )

        updateAndPersistItem(lastUsedId, item)

        val job = scope.launch {
            try {
                buffersMutex.withLock {
                    byteBuffers[id] = mutableListOf()
                }

                storageRepository.downloadFileAsFlow(bucketId, path).collect { status ->
                    when (status) {
                        is StorageDownloadStatus.Progress -> {
                            val currentItem = _downloads.value.find { it.id == id }
                            currentItem?.copy(
                                downloadedSize = status.totalBytesReceived,
                                totalSize = status.contentLength,
                                status = DownloadStatus.Downloading
                            )?.let { updateAndPersistItem(lastUsedId, it) }
                        }
                        is StorageDownloadStatus.ByteData -> {
                            buffersMutex.withLock {
                                byteBuffers[id]?.add(status.data)
                            }
                            // Update downloadedSize based on accumulated data
                            val currentSize = buffersMutex.withLock {
                                byteBuffers[id]?.sumOf { chunk -> chunk.size.toLong() } ?: 0L
                            }
                            val currentItem = _downloads.value.find { it.id == id }
                            currentItem?.copy(downloadedSize = currentSize)?.let { updateAndPersistItem(lastUsedId, it) }
                        }
                        StorageDownloadStatus.Success -> {
                        }
                    }
                }

                // Download flow completed normally - write the file
                val buffers = buffersMutex.withLock {
                    byteBuffers.remove(id) ?: mutableListOf()
                }
                
                val allBytes = concatenate(buffers)
                // A write failure must fail the download - it used to be swallowed and the item
                // was still marked as Completed even though nothing landed on disk.
                destinationFile.write(allBytes)

                val currentItem = _downloads.value.find { it.id == id }
                currentItem?.copy(
                    status = DownloadStatus.Completed,
                    downloadedTime = Clock.System.now(),
                    downloadedSize = allBytes.size.toLong(),
                    errorMessage = null
                )?.let { updateAndPersistItem(lastUsedId, it) }
            } catch (e: CancellationException) {
                // pause()/cancel() clear the buffer for us - suspending here would only fail again
                throw e
            } catch (e: Exception) {
                buffersMutex.withLock {
                    byteBuffers.remove(id)
                }
                failDownload(lastUsedId, id, bucketId, path, e)
            }
        }
        downloadJobs[id] = job
    }

    private fun failDownload(
        credentialId: String,
        id: String,
        bucketId: String,
        path: String,
        error: Throwable
    ) {
        val message = storageErrorMessage(error, StorageOperation.Download)
        logger.e(error) { "Download failed for $bucketId/$path: $message" }
        val currentItem = _downloads.value.find { it.id == id }
        currentItem?.copy(
            status = DownloadStatus.Error,
            errorMessage = message
        )?.let { updateAndPersistItem(credentialId, it) }
    }

    private fun concatenate(chunks: List<ByteArray>): ByteArray {
        val result = ByteArray(chunks.sumOf { it.size })
        var offset = 0
        for (chunk in chunks) {
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }

    private fun updateAndPersistItem(credentialId: String, item: DownloadItem) {
        val currentLastUsedId = credentialRepository.getLastUsedId()
        if (credentialId == currentLastUsedId) {
            _downloads.update { list ->
                val existing = list.indexOfFirst { it.id == item.id }
                if (existing >= 0) list.toMutableList().also { it[existing] = item }
                else list + item
            }
        }
        dbWriteChannel.trySend(DownloadWriteTask(credentialId, item))
    }

    fun pause(id: String) {
        val lastUsedId = credentialRepository.getLastUsedId() ?: return
        downloadJobs[id]?.cancel()
        scope.launch {
            buffersMutex.withLock {
                byteBuffers.remove(id)
            }
        }
        val currentItem = _downloads.value.find { it.id == id }
        currentItem?.copy(status = DownloadStatus.Paused)?.let { updateAndPersistItem(lastUsedId, it) }
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
