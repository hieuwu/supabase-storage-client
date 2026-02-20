package com.hieuwu.supabasestorageclient.domain.download

import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.util.FileWriter
import com.hieuwu.supabasestorageclient.util.PermissionManager
import io.github.jan.supabase.storage.DownloadStatus as SupabaseDownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class DownloadManager(
    private val storageRepository: StorageRepository,
    private val fileWriter: FileWriter,
    private val permissionManager: PermissionManager
) {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    // Accumulate byte data across chunks
    private val byteBuffers = mutableMapOf<String, MutableList<ByteArray>>()
    private val buffersMutex = Mutex()

    fun download(bucketId: String, path: String, fileName: String, destinationPath: String) {
        val id = "$bucketId:$path"
        // Don't start duplicate active downloads
        if (_downloads.value.any { it.id == id && it.status == DownloadStatus.Downloading }) return

        val item = DownloadItem(
            id = id,
            fileName = fileName,
            bucketId = bucketId,
            path = path,
            totalSize = 0,
            destinationPath = destinationPath
        )

        // Replace or add item
        _downloads.update { list ->
            val existing = list.indexOfFirst { it.id == id }
            if (existing >= 0) list.toMutableList().also { it[existing] = item }
            else list + item
        }

        val job = scope.launch {
            try {
                buffersMutex.withLock {
                    byteBuffers[id] = mutableListOf()
                }

                if (!permissionManager.requestStoragePermission()) {
                    _downloads.update { list ->
                        list.map {
                            if (it.id == id) it.copy(status = DownloadStatus.Error) else it
                        }
                    }
                    return@launch
                }

                storageRepository.downloadFileAsFlow(bucketId, path).collect { status ->
                    // println("DownloadManager: Received status: ${status::class.simpleName}")
                    when (status) {
                        is SupabaseDownloadStatus.Progress -> {
                            _downloads.update { list ->
                                list.map {
                                    if (it.id == id) it.copy(
                                        downloadedSize = status.totalBytesReceived,
                                        totalSize = status.contentLength,
                                        status = DownloadStatus.Downloading
                                    ) else it
                                }
                            }
                        }
                        is SupabaseDownloadStatus.ByteData -> {
                            buffersMutex.withLock {
                                byteBuffers[id]?.add(status.data)
                            }
                            // Update downloadedSize based on accumulated data
                            val currentSize = buffersMutex.withLock {
                                byteBuffers[id]?.sumOf { chunk -> chunk.size.toLong() } ?: 0L
                            }
                            _downloads.update { list ->
                                list.map {
                                    if (it.id == id) it.copy(downloadedSize = currentSize) else it
                                }
                            }
                        }
                        SupabaseDownloadStatus.Success -> {
                            // Just mark status as completed in UI if we want, 
                            // but we will do the final write after collect finishes.
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

                    val fullPath = if (destinationPath.endsWith("/")) {
                        "$destinationPath$fileName"
                    } else {
                        "$destinationPath/$fileName"
                    }
                    
                    fileWriter.writeToFile(fullPath, allBytes)

                    _downloads.update { list ->
                        list.map {
                            if (it.id == id) it.copy(
                                status = DownloadStatus.Completed,
                                downloadedTime = Clock.System.now(),
                                downloadedSize = totalSizeBytes.toLong()
                            ) else it
                        }
                    }
                } else {
                    // No data received? If it reached here without error, maybe it was a 0-byte file.
                    // But usually, we should have at least one ByteData event if the file has content.
                    _downloads.update { list ->
                        list.map {
                            if (it.id == id) it.copy(status = DownloadStatus.Completed) else it
                        }
                    }
                }

            } catch (e: Exception) {
                buffersMutex.withLock {
                    byteBuffers.remove(id)
                }
                _downloads.update { list ->
                    list.map {
                        if (it.id == id) it.copy(status = DownloadStatus.Error) else it
                    }
                }
            }
        }
        downloadJobs[id] = job
    }

    fun pause(id: String) {
        downloadJobs[id]?.cancel()
        scope.launch {
            buffersMutex.withLock {
                byteBuffers.remove(id)
            }
        }
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.Paused) else it }
        }
    }

    fun resume(id: String) {
        val item = _downloads.value.find { it.id == id } ?: return
        download(item.bucketId, item.path, item.fileName, item.destinationPath)
    }

    fun cancel(id: String) {
        downloadJobs[id]?.cancel()
        scope.launch {
            buffersMutex.withLock {
                byteBuffers.remove(id)
            }
        }
        _downloads.update { it.filter { item -> item.id != id } }
    }
}
