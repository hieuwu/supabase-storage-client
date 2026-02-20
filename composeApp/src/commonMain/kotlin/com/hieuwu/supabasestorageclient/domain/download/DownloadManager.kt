package com.hieuwu.supabasestorageclient.domain.download

import com.hieuwu.supabasestorageclient.domain.model.DownloadItem
import com.hieuwu.supabasestorageclient.domain.model.DownloadStatus
import com.hieuwu.supabasestorageclient.domain.repository.StorageRepository
import com.hieuwu.supabasestorageclient.util.FileWriter
import io.github.jan.supabase.storage.DownloadStatus as SupabaseDownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class DownloadManager(
    private val storageRepository: StorageRepository,
    private val fileWriter: FileWriter
) {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    // Accumulate byte data across chunks
    private val byteBuffers = mutableMapOf<String, MutableList<ByteArray>>()

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
        byteBuffers[id] = mutableListOf()

        val job = scope.launch {
            try {
                storageRepository.downloadFileAsFlow(bucketId, path).collect { status ->
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
                            byteBuffers[id]?.add(status.data)
                        }
                        SupabaseDownloadStatus.Success -> {
                            val allBytes = byteBuffers[id]
                                ?.fold(ByteArray(0)) { acc, b -> acc + b }
                                ?: ByteArray(0)
                            byteBuffers.remove(id)

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
                                        downloadedSize = it.totalSize
                                    ) else it
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                byteBuffers.remove(id)
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
        byteBuffers.remove(id)
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
        byteBuffers.remove(id)
        _downloads.update { it.filter { item -> item.id != id } }
    }
}
