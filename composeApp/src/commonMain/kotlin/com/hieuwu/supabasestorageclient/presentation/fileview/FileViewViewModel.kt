package com.hieuwu.supabasestorageclient.presentation.fileview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.AskDownloadPathConfig
import com.hieuwu.supabasestorageclient.domain.model.UserSettings
import com.hieuwu.supabasestorageclient.domain.usecase.*
import com.hieuwu.supabasestorageclient.core.FileUtils
import com.hieuwu.supabasestorageclient.observability.analytics.AnalyticsSurfaces
import com.hieuwu.supabasestorageclient.observability.analytics.AppAnalytics
import com.hieuwu.supabasestorageclient.observability.analytics.DownloadDestinationModes
import com.hieuwu.supabasestorageclient.observability.analytics.LinkTypes
import com.hieuwu.supabasestorageclient.observability.analytics.PreviewTypes
import com.hieuwu.supabasestorageclient.observability.analytics.StorageActions
import com.hieuwu.supabasestorageclient.observability.analytics.logFileLinkCopied
import com.hieuwu.supabasestorageclient.observability.analytics.logFilePreviewOpened
import com.hieuwu.supabasestorageclient.observability.analytics.logStorageAction
import com.hieuwu.supabasestorageclient.platform.ClipboardManager
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
        // Which viewers are worth maintaining, and how often people land on a type we cannot render.
        AppAnalytics.logFilePreviewOpened(fileName, previewTypeOf(fileName))
    }

    private fun previewTypeOf(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        return when {
            FileUtils.isGif(extension) -> PreviewTypes.GIF
            FileUtils.isImage(extension) -> PreviewTypes.IMAGE
            FileUtils.isVideo(extension) -> PreviewTypes.VIDEO
            FileUtils.isPdf(extension) -> PreviewTypes.PDF
            else -> PreviewTypes.UNSUPPORTED
        }
    }

    /** Reads the settings once; returns null (after logging) when they cannot be read. */
    private suspend fun currentSettings(): UserSettings? =
        runCatching { getUserSettingsUseCase().first() }
            .onFailure { error -> logger.e(error) { "Failed to read user settings" } }
            .getOrNull()

    /**
     * Remembering the directory is a convenience - failing to store it must not stop the download
     * the user just asked for.
     */
    private suspend fun rememberDownloadDirectory(settings: UserSettings, directory: String) {
        updateUserSettingsUseCase(settings.copy(sessionDownloadDirectory = directory))
            .onFailure { error -> logger.w(error) { "Failed to remember download directory" } }
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
            val settings = currentSettings() ?: return@launch
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"

            when (settings.askDownloadPathConfig) {
                AskDownloadPathConfig.NEVER_ASK -> {
                    if (settings.defaultDownloadDirectory != null) {
                        downloadFileUseCase.downloadToPath(
                            bucketId, fullPath, fileName, settings.defaultDownloadDirectory,
                            DownloadDestinationModes.DEFAULT_FOLDER,
                        )
                        _manualState.update { it.copy(successMessage = "Download started") }
                    } else {
                        _manualState.update { it.copy(itemToDownload = item, isSavingFile = true) }
                    }
                }
                AskDownloadPathConfig.ONCE_WHEN_APP_OPEN -> {
                    if (settings.sessionDownloadDirectory != null) {
                        downloadFileUseCase.downloadToPath(
                            bucketId, fullPath, fileName, settings.sessionDownloadDirectory,
                            DownloadDestinationModes.SESSION_FOLDER,
                        )
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
            val settings = currentSettings() ?: return@launch
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"

            if (settings.defaultDownloadDirectory != null) {
                rememberDownloadDirectory(settings, settings.defaultDownloadDirectory)
                downloadFileUseCase.downloadToPath(
                    bucketId, fullPath, fileName, settings.defaultDownloadDirectory,
                    DownloadDestinationModes.DEFAULT_FOLDER,
                )
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
            val settings = currentSettings() ?: return@launch
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"

            rememberDownloadDirectory(settings, pickedPath)
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
                    AppAnalytics.logStorageAction(StorageActions.DELETE_FILE)
                    _manualState.update { it.copy(
                        isDeleted = true,
                        isLoading = false,
                        successMessage = "File deleted successfully"
                    ) }
                },
                onFailure = { error ->
                    AppAnalytics.logStorageAction(StorageActions.DELETE_FILE, error)
                    logger.e(error) { "Failed to delete file $fullPath" }
                    _manualState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun copyUrl() {
        val url = _manualState.value.publicUrl ?: return
        runCatching { clipboardManager.copyText(url) }.fold(
            onSuccess = {
                AppAnalytics.logFileLinkCopied(LinkTypes.PUBLIC_URL, AnalyticsSurfaces.FILE_VIEW)
                _manualState.update { it.copy(successMessage = "URL copied to clipboard") }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to copy the public url to the clipboard" }
                _manualState.update { it.copy(error = "Could not copy the URL") }
            }
        )
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
