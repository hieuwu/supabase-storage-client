package com.hieuwu.supabasestorageclient.presentation.fileview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetFileMetadataUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetPublicUrlUseCase
import com.hieuwu.supabasestorageclient.util.ClipboardManager
import com.hieuwu.supabasestorageclient.util.DirectoryPicker
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileViewUiState(
    val bucketId: String = "",
    val fileName: String = "",
    val path: String? = null,
    val publicUrl: String? = null,
    val metadata: StorageItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val successMessage: String? = null
)

class FileViewViewModel(
    private val bucketId: String,
    private val fileName: String,
    private val path: String?,
    private val getPublicUrlUseCase: GetPublicUrlUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val getFileMetadataUseCase: GetFileMetadataUseCase,
    private val clipboardManager: ClipboardManager,
    private val downloadManager: DownloadManager,
    private val directoryPicker: DirectoryPicker,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileViewUiState(bucketId = bucketId, fileName = fileName, path = path))
    val uiState: StateFlow<FileViewUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            
            getPublicUrlUseCase(bucketId, fullPath).fold(
                onSuccess = { url ->
                    _uiState.update { it.copy(publicUrl = url) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to get public URL for $fullPath" }
                    _uiState.update { it.copy(error = error.message) }
                }
            )

            getFileMetadataUseCase(bucketId, fullPath).fold(
                onSuccess = { metadata ->
                    _uiState.update { it.copy(metadata = metadata, isLoading = false) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to get metadata for $fullPath" }
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun downloadFile() {
        viewModelScope.launch {
            val destDir = directoryPicker.pickDirectory()
            if (destDir == null) {
                // User cancelled the picker
                return@launch
            }
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            downloadManager.download(
                bucketId = bucketId,
                path = fullPath,
                fileName = fileName,
                destinationPath = destDir
            )
            _uiState.update { it.copy(successMessage = "Download started") }
        }
    }

    fun deleteFile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            deleteFileUseCase(bucketId, fullPath).fold(
                onSuccess = {
                    _uiState.update { it.copy(
                        isDeleted = true,
                        isLoading = false,
                        successMessage = "File deleted successfully"
                    ) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete file $fullPath" }
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun copyUrl() {
        uiState.value.publicUrl?.let { url ->
            clipboardManager.copyText(url)
            _uiState.update { it.copy(successMessage = "URL copied to clipboard") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
