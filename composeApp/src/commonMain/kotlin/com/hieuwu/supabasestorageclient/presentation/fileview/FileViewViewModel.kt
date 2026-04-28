package com.hieuwu.supabasestorageclient.presentation.fileview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.AskDownloadPathConfig
import com.hieuwu.supabasestorageclient.domain.usecase.*
import com.hieuwu.supabasestorageclient.util.ClipboardManager
import co.touchlab.kermit.Logger
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FileViewViewModel(
    private val bucketId: String,
    private val fileName: String,
    private val path: String?,
    private val getPublicUrlUseCase: GetPublicUrlUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val getFileMetadataUseCase: GetFileMetadataUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val clipboardManager: ClipboardManager,
    private val logger: Logger
) : ViewModel() {

    private val _manualState = MutableStateFlow(ManualFileViewState())

    val uiState: StateFlow<FileViewUiState> = combine(
        getUserSettingsUseCase(),
        _manualState
    ) { settings, manual ->
        if (manual.isLoading && manual.metadata == null) {
            FileViewUiState.Loading
        } else if (manual.error != null && manual.metadata == null) {
            FileViewUiState.Error(manual.error)
        } else {
            FileViewUiState.Content(
                bucketId = bucketId,
                fileName = fileName,
                path = path,
                publicUrl = manual.publicUrl,
                metadata = manual.metadata,
                isDeleted = manual.isDeleted,
                successMessage = manual.successMessage,
                error = manual.error,
                itemToDownload = manual.itemToDownload,
                isPickingDirectory = manual.isPickingDirectory,
                isSavingFile = manual.isSavingFile,
                showDownloadPathOptionDialog = manual.showDownloadPathOptionDialog,
                defaultDownloadPath = settings.defaultDownloadDirectory
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FileViewUiState.Loading)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _manualState.update { it.copy(isLoading = true) }
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            
            val publicUrlParams = GetPublicUrlUseCase.Params(bucketId = bucketId, path = fullPath)
            getPublicUrlUseCase(publicUrlParams).fold(
                onSuccess = { url ->
                    _manualState.update { it.copy(publicUrl = url) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to get public URL for $fullPath" }
                    _manualState.update { it.copy(error = error.message) }
                }
            )

            val metadataParams = GetFileMetadataUseCase.Params(bucketId = bucketId, path = fullPath)
            getFileMetadataUseCase(metadataParams).fold(
                onSuccess = { metadata ->
                    _manualState.update { it.copy(metadata = metadata, isLoading = false) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to get metadata for $fullPath" }
                    _manualState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun downloadFile() {
        val currentState = uiState.value as? FileViewUiState.Content ?: return
        val item = currentState.metadata ?: return
        viewModelScope.launch {
            val settings = getUserSettingsUseCase().first()
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"

            when (settings.askDownloadPathConfig) {
                AskDownloadPathConfig.NEVER_ASK -> {
                    if (settings.defaultDownloadDirectory != null) {
                        downloadFileUseCase.downloadToPath(bucketId, fullPath, fileName, settings.defaultDownloadDirectory)
                        _manualState.update { it.copy(successMessage = "Download started") }
                    } else {
                        _manualState.update { it.copy(itemToDownload = item, isSavingFile = true) }
                    }
                }
                AskDownloadPathConfig.ONCE_WHEN_APP_OPEN -> {
                    if (settings.sessionDownloadDirectory != null) {
                        downloadFileUseCase.downloadToPath(bucketId, fullPath, fileName, settings.sessionDownloadDirectory)
                        _manualState.update { it.copy(successMessage = "Download started") }
                    } else {
                        _manualState.update { it.copy(
                            showDownloadPathOptionDialog = true, 
                            itemToDownload = item
                        ) }
                    }
                }
                AskDownloadPathConfig.ASK_EVERYTIME -> {
                    _manualState.update { it.copy(
                        showDownloadPathOptionDialog = true,
                        itemToDownload = item
                    ) }
                }
            }
        }
    }

    fun onSelectDefaultPath() {
        viewModelScope.launch {
            val settings = getUserSettingsUseCase().first()
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            
            if (settings.defaultDownloadDirectory != null) {
                updateUserSettingsUseCase(settings.copy(sessionDownloadDirectory = settings.defaultDownloadDirectory))
                downloadFileUseCase.downloadToPath(bucketId, fullPath, fileName, settings.defaultDownloadDirectory)
                _manualState.update { it.copy(successMessage = "Download started", showDownloadPathOptionDialog = false, itemToDownload = null) }
            } else {
                _manualState.update { it.copy(showDownloadPathOptionDialog = false, isPickingDirectory = true) }
            }
        }
    }

    fun onSelectCustomPath() {
        _manualState.update { it.copy(showDownloadPathOptionDialog = false, isPickingDirectory = true) }
    }

    fun onCancelDownload() {
        _manualState.update { it.copy(showDownloadPathOptionDialog = false, itemToDownload = null) }
    }

    fun onDirectoryPicked(pickedPath: String) {
        viewModelScope.launch {
            val settings = getUserSettingsUseCase().first()
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            
            updateUserSettingsUseCase(settings.copy(sessionDownloadDirectory = pickedPath))
            downloadFileUseCase.downloadToPath(bucketId, fullPath, fileName, pickedPath)
            _manualState.update { it.copy(successMessage = "Download started", isPickingDirectory = false, itemToDownload = null) }
        }
    }

    fun onDirectoryPickingCancelled() {
        _manualState.update { it.copy(isPickingDirectory = false, itemToDownload = null) }
    }
    
    fun onFileSaved() {
        _manualState.update { it.copy(isSavingFile = false, itemToDownload = null) }
    }

    fun startDownload(platformFile: PlatformFile) {
        viewModelScope.launch {
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            downloadFileUseCase.download(
                bucketId = bucketId,
                path = fullPath,
                fileName = fileName,
                platformFile = platformFile
            )
            _manualState.update { it.copy(successMessage = "Download started", isSavingFile = false, itemToDownload = null) }
        }
    }

    fun clearItemToDownload() {
        _manualState.update { it.copy(itemToDownload = null, isSavingFile = false, showDownloadPathOptionDialog = false) }
    }

    fun deleteFile() {
        viewModelScope.launch {
            _manualState.update { it.copy(isLoading = true) }
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            val params = DeleteFileUseCase.Params(bucketId = bucketId, path = fullPath)
            deleteFileUseCase(params).fold(
                onSuccess = {
                    _manualState.update { it.copy(
                        isDeleted = true,
                        isLoading = false,
                        successMessage = "File deleted successfully"
                    ) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete file $fullPath" }
                    _manualState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun copyUrl() {
        _manualState.value.publicUrl?.let { url ->
            clipboardManager.copyText(url)
            _manualState.update { it.copy(successMessage = "URL copied to clipboard") }
        }
    }

    fun clearMessages() {
        _manualState.update { it.copy(error = null, successMessage = null) }
    }
}

data class ManualFileViewState(
    val publicUrl: String? = null,
    val metadata: com.hieuwu.supabasestorageclient.domain.model.StorageItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDeleted: Boolean = false,
    val successMessage: String? = null,
    val itemToDownload: com.hieuwu.supabasestorageclient.domain.model.StorageItem? = null,
    val isPickingDirectory: Boolean = false,
    val isSavingFile: Boolean = false,
    val showDownloadPathOptionDialog: Boolean = false
)
