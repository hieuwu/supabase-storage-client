package com.hieuwu.supabasestorageclient.domain.download

import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.error.StorageOperation
import com.hieuwu.supabasestorageclient.domain.error.storageErrorMessage
import com.hieuwu.supabasestorageclient.domain.error.storageErrorReason
import com.hieuwu.supabasestorageclient.observability.analytics.AppAnalytics
import com.hieuwu.supabasestorageclient.observability.analytics.DownloadDestinationModes
import com.hieuwu.supabasestorageclient.observability.analytics.logDownloadCancelled
import com.hieuwu.supabasestorageclient.observability.analytics.logDownloadFailed
import com.hieuwu.supabasestorageclient.observability.analytics.logDownloadStarted
import com.hieuwu.supabasestorageclient.observability.analytics.logDownloadSucceeded
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
                // A single failed write must not kill the consumer - it would silently stop
                // persisting every later progress update.
                runCatching { downloadRepository.insertDownloadItem(task.credentialId, task.item) }
                    .onFailure { error ->
                        logger.e(error) { "Failed to persist download ${task.item.id}" }
                    }
            }
        }
        scope.launch {
            credentialRepository.lastUsedId.collectLatest { lastUsedId ->
                if (lastUsedId != null) {
                    runCatching {
                        downloadRepository.getDownloadItems(lastUsedId).collect { items ->
                            _downloads.value = items
                        }
                    }.onFailure { error ->
                        // Stop here if the collector itself was cancelled - there is nothing to report.
                        currentCoroutineContext().ensureActive()
                        logger.e(error) { "Failed to observe downloads for $lastUsedId" }
                    }
                } else {
                    _downloads.value = emptyList()
                }
            }
        }
    }

    /** Downloads into a file the user already picked. */
    fun download(
        bucketId: String,
        path: String,
        fileName: String,
        platformFile: PlatformFile,
        destinationMode: String = DownloadDestinationModes.PICKED_FILE,
    ) {
        startDownload(bucketId, path, fileName, platformFile, platformFile.path, destinationMode)
    }

    /** Downloads into [fileName] inside a directory the user already picked. */
    fun downloadToDirectoryPath(
        bucketId: String,
        path: String,
        fileName: String,
        destinationDirectory: String,
        destinationMode: String = DownloadDestinationModes.PICKED_FOLDER,
    ) {
        // The picked directory is not always a file system path - on Android it is a Storage
        // Access Framework uri, and resolving the child then queries the ContentResolver, which
        // throws once the granted permission is revoked or the folder is gone. That happens before
        // any DownloadItem exists, so there is nothing to attach the error to but the log.
        val destinationFile = runCatching { PlatformFile(PlatformFile(destinationDirectory), fileName) }
            .getOrElse { error ->
                logger.e(error) { "Cannot resolve download destination '$destinationDirectory/$fileName'" }
                return
            }
        startDownload(bucketId, path, fileName, destinationFile, destinationFile.path, destinationMode)
    }

    private fun startDownload(
        bucketId: String,
        path: String,
        fileName: String,
        destinationFile: PlatformFile,
        destinationPath: String,
        destinationMode: String
    ) {
        val lastUsedId = credentialRepository.getLastUsedId() ?: return
        val id = "$bucketId:$path"
        // Don't start duplicate active downloads
        if (_downloads.value.any { it.id == id && it.status == DownloadStatus.Downloading }) return

        updateAndPersistItem(
            lastUsedId,
            DownloadItem(
                id = id,
                fileName = fileName,
                bucketId = bucketId,
                path = path,
                from = "$bucketId/$path",
                totalSize = 0,
                destinationPath = destinationPath,
                sourcePath = path.substringBeforeLast("/", "")
            )
        )

        AppAnalytics.logDownloadStarted(fileName, destinationMode)
        val startedAt = Clock.System.now()

        val job = scope.launch {
            runCatching {
                buffersMutex.withLock {
                    byteBuffers[id] = mutableListOf()
                }

                storageRepository.downloadFileAsFlow(bucketId, path).collect { status ->
                    when (status) {
                        // Progress carries the content length, which is the only place the total
                        // size comes from; the running byte count comes from ByteData below.
                        is StorageDownloadStatus.Progress -> {
                            _downloads.value.find { it.id == id }
                                ?.copy(totalSize = status.contentLength)
                                ?.let { updateAndPersistItem(lastUsedId, it) }
                        }

                        is StorageDownloadStatus.ByteData -> {
                            val currentSize = buffersMutex.withLock {
                                val buffer = byteBuffers[id] ?: return@withLock 0L
                                buffer.add(status.data)
                                buffer.sumOf { chunk -> chunk.size.toLong() }
                            }
                            _downloads.value.find { it.id == id }
                                ?.copy(downloadedSize = currentSize)
                                ?.let { updateAndPersistItem(lastUsedId, it) }
                        }

                        // Only marks the end of the stream - the file is written once collect
                        // returns, so there is nothing to do here.
                        StorageDownloadStatus.Success -> Unit
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

                _downloads.value.find { it.id == id }?.copy(
                    status = DownloadStatus.Completed,
                    downloadedTime = Clock.System.now(),
                    downloadedSize = allBytes.size.toLong(),
                    errorMessage = null
                )?.let { updateAndPersistItem(lastUsedId, it) }

                AppAnalytics.logDownloadSucceeded(
                    fileName = fileName,
                    sizeBytes = allBytes.size.toLong(),
                    durationMs = Clock.System.now().toEpochMilliseconds() -
                        startedAt.toEpochMilliseconds(),
                )
            }.onFailure { error ->
                // pause()/cancel() clear the buffer for us - suspending here would only fail again
                currentCoroutineContext().ensureActive()
                buffersMutex.withLock {
                    byteBuffers.remove(id)
                }
                AppAnalytics.logDownloadFailed(fileName, storageErrorReason(error))
                failDownload(lastUsedId, id, bucketId, path, error)
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

    /**
     * Not implemented: only the destination *path* is stored, not the `PlatformFile` handle, so a
     * paused download cannot be restarted without the user picking the destination again. Left as
     * a no-op that says so, rather than silently doing nothing - there are currently no callers.
     */
    fun resume(id: String) {
        logger.w { "Resume is not supported yet - download $id stays paused" }
    }

    fun cancel(id: String) {
        // deleteDownload() routes here too - only an in-flight transfer counts as a cancellation.
        if (_downloads.value.any { it.id == id && it.status == DownloadStatus.Downloading }) {
            AppAnalytics.logDownloadCancelled()
        }
        downloadJobs[id]?.cancel()
        scope.launch {
            buffersMutex.withLock {
                byteBuffers.remove(id)
            }
            val lastUsedId = credentialRepository.getLastUsedId()
            if (lastUsedId != null) {
                runCatching { downloadRepository.deleteDownloadItem(lastUsedId, id) }
                    .onFailure { error -> logger.e(error) { "Failed to delete download $id" } }
            }
        }
        _downloads.update { it.filter { item -> item.id != id } }
    }

    fun deleteDownload(id: String) {
        cancel(id)
    }
}
