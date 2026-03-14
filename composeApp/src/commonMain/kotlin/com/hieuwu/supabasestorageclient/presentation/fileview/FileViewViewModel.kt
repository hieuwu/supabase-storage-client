package com.hieuwu.supabasestorageclient.presentation.fileview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.usecase.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetFileMetadataUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetPublicUrlUseCase
import com.hieuwu.supabasestorageclient.util.ClipboardManager
import com.hieuwu.supabasestorageclient.domain.repository.SettingsRepository
import com.hieuwu.supabasestorageclient.domain.model.AskDownloadPathConfig
import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FileViewViewModel(
    private val bucketId: String,
    private val fileName: String,
    private val path: String?,
    private val getPublicUrlUseCase: GetPublicUrlUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val getFileMetadataUseCase: GetFileMetadataUseCase,
    private val clipboardManager: ClipboardManager,
    private val downloadManager: DownloadManager,
    private val settingsRepository: SettingsRepository,
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
            
            val publicUrlParams = GetPublicUrlUseCase.Params(bucketId = bucketId, path = fullPath)
            getPublicUrlUseCase(publicUrlParams).fold(
                onSuccess = { url ->
                    _uiState.update { it.copy(publicUrl = url) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to get public URL for $fullPath" }
                    _uiState.update { it.copy(error = error.message) }
                }
            )

            val metadataParams = GetFileMetadataUseCase.Params(bucketId = bucketId, path = fullPath)
            getFileMetadataUseCase(metadataParams).fold(
                onSuccess = { metadata ->
                    viewModelScope.launch {
                        val settings = settingsRepository.getSettings().firstOrNull()
                        _uiState.update { it.copy(
                            metadata = metadata, 
                            isLoading = false,
                            defaultDownloadPath = settings?.defaultDownloadDirectory
                        ) }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to get metadata for $fullPath" }
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun downloadFile() {
        val item = _uiState.value.metadata ?: return
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().firstOrNull() ?: return@launch
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"

            when (settings.askDownloadPathConfig) {
                AskDownloadPathConfig.NEVER_ASK -> {
                    if (settings.defaultDownloadDirectory != null) {
                        downloadManager.downloadToDirectoryPath(bucketId, fullPath, fileName, settings.defaultDownloadDirectory)
                        _uiState.update { it.copy(successMessage = "Download started") }
                    } else {
                        _uiState.update { it.copy(itemToDownload = item, isSavingFile = true) }
                    }
                }
                AskDownloadPathConfig.ONCE_WHEN_APP_OPEN -> {
                    if (settings.sessionDownloadDirectory != null) {
                        downloadManager.downloadToDirectoryPath(bucketId, fullPath, fileName, settings.sessionDownloadDirectory)
                        _uiState.update { it.copy(successMessage = "Download started") }
                    } else {
                        _uiState.update { it.copy(
                            showDownloadPathOptionDialog = true, 
                            itemToDownload = item,
                            defaultDownloadPath = settings.defaultDownloadDirectory
                        ) }
                    }
                }
                AskDownloadPathConfig.ASK_EVERYTIME -> {
                    _uiState.update { it.copy(itemToDownload = item, isSavingFile = true) }
                }
            }
        }
    }

    fun onSelectDefaultPath() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().firstOrNull() ?: return@launch
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            
            if (settings.defaultDownloadDirectory != null) {
                settingsRepository.updateSettings(settings.copy(sessionDownloadDirectory = settings.defaultDownloadDirectory))
                downloadManager.downloadToDirectoryPath(bucketId, fullPath, fileName, settings.defaultDownloadDirectory)
                _uiState.update { it.copy(successMessage = "Download started", showDownloadPathOptionDialog = false, itemToDownload = null) }
            } else {
                _uiState.update { it.copy(showDownloadPathOptionDialog = false, isPickingDirectory = true) }
            }
        }
    }

    fun onSelectCustomPath() {
        _uiState.update { it.copy(showDownloadPathOptionDialog = false, isPickingDirectory = true) }
    }

    fun onCancelDownload() {
        _uiState.update { it.copy(showDownloadPathOptionDialog = false, itemToDownload = null) }
    }

    fun onDirectoryPicked(pickedPath: String) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings().firstOrNull() ?: return@launch
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            
            settingsRepository.updateSettings(settings.copy(sessionDownloadDirectory = pickedPath))
            downloadManager.downloadToDirectoryPath(bucketId, fullPath, fileName, pickedPath)
            _uiState.update { it.copy(successMessage = "Download started", isPickingDirectory = false, itemToDownload = null) }
        }
    }

    fun onDirectoryPickingCancelled() {
        _uiState.update { it.copy(isPickingDirectory = false, itemToDownload = null) }
    }
    
    fun onFileSaved() {
        _uiState.update { it.copy(isSavingFile = false, itemToDownload = null) }
    }

    fun startDownload(platformFile: PlatformFile) {
        viewModelScope.launch {
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            downloadManager.download(
                bucketId = bucketId,
                path = fullPath,
                fileName = fileName,
                platformFile = platformFile
            )
            _uiState.update { it.copy(successMessage = "Download started", isSavingFile = false, itemToDownload = null) }
        }
    }

    fun clearItemToDownload() {
        _uiState.update { it.copy(itemToDownload = null, isSavingFile = false, showDownloadPathOptionDialog = false) }
    }

    fun deleteFile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            val params = DeleteFileUseCase.Params(bucketId = bucketId, path = fullPath)
            deleteFileUseCase(params).fold(
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
