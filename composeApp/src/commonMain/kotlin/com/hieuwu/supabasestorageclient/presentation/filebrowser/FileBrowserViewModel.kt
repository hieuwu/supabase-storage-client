package com.hieuwu.supabasestorageclient.presentation.filebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.usecase.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.MoveFileUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetPublicUrlUseCase
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.domain.usecase.RefreshBucketContentsUseCase
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.usecase.GetStarredItemsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.ToggleStarUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetUserSettingsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.UpdateUserSettingsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.hieuwu.supabasestorageclient.domain.model.AskDownloadPathConfig
import com.hieuwu.supabasestorageclient.platform.ClipboardManager
import io.github.vinceglb.filekit.PlatformFile

class FileBrowserViewModel(
    private val bucketId: String,
    private val path: String?,
    private val getBucketContentsUseCase: GetBucketContentsUseCase,
    private val refreshBucketContentsUseCase: RefreshBucketContentsUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val moveFileUseCase: MoveFileUseCase,
    private val getPublicUrlUseCase: GetPublicUrlUseCase,
    private val clipboardManager: ClipboardManager,
    private val downloadManager: DownloadManager,
    private val getStarredItemsUseCase: GetStarredItemsUseCase,
    private val toggleStarUseCase: ToggleStarUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val contextSelectionManager: ContextSelectionManager,
    private val logger: Logger
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    private val _manualState = MutableStateFlow(ManualUiState())

    val uiState: StateFlow<BucketUiState> = combine(
        refreshTrigger.flatMapLatest {
            combine(
                getStarredItemsUseCase(),
                getUserSettingsUseCase(),
                flow { emit(getBucketContentsUseCase(GetBucketContentsUseCase.Params(bucketId, path.orEmpty()))) }
            ) { stars, settings, result ->
                Triple(stars, settings, result)
            }
        },
        _manualState
    ) { (stars, settings, result), manual ->
        result.fold(
            onSuccess = { items ->
                val starredPaths = stars.filter { it.bucketId == bucketId }.map { it.path ?: "" }.toSet()
                val updatedItems = items.map { item ->
                    item.copy(isStarred = starredPaths.contains(item.getFullPath(path)))
                }
                BucketUiState.Content(
                    items = updatedItems,
                    viewMode = settings.viewMode,
                    defaultDownloadPath = settings.defaultDownloadDirectory,
                    itemToDownload = manual.itemToDownload,
                    isPickingDirectory = manual.isPickingDirectory,
                    isSavingFile = manual.isSavingFile,
                    showDownloadPathOptionDialog = manual.showDownloadPathOptionDialog,
                    successMessage = manual.successMessage,
                    error = manual.error
                )
            },
            onFailure = { error ->
                BucketUiState.Error(error.message ?: "Unknown error")
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BucketUiState.Loading)

    init {
        contextSelectionManager.setContext(bucketId, path ?: "")
    }

    fun refreshContents() {
        viewModelScope.launch {
            refreshBucketContentsUseCase(bucketId)
                .onSuccess {
                    refreshTrigger.emit(Unit)
                }
                .onFailure { error ->
                    logger.e(error) { "Failed to refresh contents for bucket $bucketId" }
                    _manualState.update { it.copy(error = error.message) }
                }
        }
    }

    fun renameItem(oldName: String, newName: String) {
        viewModelScope.launch {
            val oldFullPath = if (path.isNullOrEmpty()) oldName else "$path/$oldName"
            val newFullPath = if (path.isNullOrEmpty()) newName else "$path/$newName"
            val params = MoveFileUseCase.Params(bucketId = bucketId, fromPath = oldFullPath, toPath = newFullPath)
            moveFileUseCase(params).fold(
                onSuccess = {
                    _manualState.update { it.copy(successMessage = "Renamed successfully") }
                    refreshTrigger.emit(Unit)
                },
                onFailure = { error ->
                    _manualState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun moveItem(oldName: String, newPath: String) {
        viewModelScope.launch {
            val oldFullPath = if (path.isNullOrEmpty()) oldName else "$path/$oldName"
            val newFullPath = if (newPath.isEmpty()) oldName else "$newPath/$oldName"
            val params = MoveFileUseCase.Params(bucketId = bucketId, fromPath = oldFullPath, toPath = newFullPath)
            moveFileUseCase(params).fold(
                onSuccess = {
                    _manualState.update { it.copy(successMessage = "Moved successfully") }
                    refreshTrigger.emit(Unit)
                },
                onFailure = { error ->
                    _manualState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun deleteItem(name: String) {
        viewModelScope.launch {
            val fullPath = if (path.isNullOrEmpty()) name else "$path/$name"
            val params = DeleteFileUseCase.Params(bucketId = bucketId, path = fullPath)
            deleteFileUseCase(params).fold(
                onSuccess = {
                    _manualState.update { it.copy(successMessage = "Deleted successfully") }
                    refreshTrigger.emit(Unit)
                },
                onFailure = { error ->
                    _manualState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun getPublicUrl(name: String) {
        viewModelScope.launch {
            val fullPath = if (path.isNullOrEmpty()) name else "$path/$name"
            val params = GetPublicUrlUseCase.Params(bucketId = bucketId, path = fullPath)
            getPublicUrlUseCase(params).fold(
                onSuccess = { url ->
                    clipboardManager.copyText(url)
                    _manualState.update { it.copy(successMessage = "URL copied to clipboard") }
                },
                onFailure = { error ->
                    _manualState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun copyPath(name: String) {
        val fullPath = if (path.isNullOrEmpty()) name else "$path/$name"
        clipboardManager.copyText(fullPath)
        _manualState.update { it.copy(successMessage = "Path copied to clipboard") }
    }

    fun downloadItem(name: String) {
        val currentState = uiState.value
        if (currentState !is BucketUiState.Content) return
        val item = currentState.items.find { it.name == name } ?: return

        viewModelScope.launch {
            val settings = getUserSettingsUseCase().firstOrNull() ?: return@launch
            val fullPath = item.getFullPath(path)

            when (settings.askDownloadPathConfig) {
                AskDownloadPathConfig.NEVER_ASK -> {
                    if (settings.defaultDownloadDirectory != null) {
                        downloadManager.downloadToDirectoryPath(bucketId, fullPath, name, settings.defaultDownloadDirectory)
                        _manualState.update { it.copy(successMessage = "Download started") }
                    } else {
                        _manualState.update { it.copy(itemToDownload = item, isSavingFile = true) }
                    }
                }
                AskDownloadPathConfig.ONCE_WHEN_APP_OPEN -> {
                    if (settings.sessionDownloadDirectory != null) {
                        downloadManager.downloadToDirectoryPath(bucketId, fullPath, name, settings.sessionDownloadDirectory)
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
        val manual = _manualState.value
        val item = manual.itemToDownload ?: return
        viewModelScope.launch {
            val settings = getUserSettingsUseCase().firstOrNull() ?: return@launch
            val fullPath = item.getFullPath(path)
            
            if (settings.defaultDownloadDirectory != null) {
                updateUserSettingsUseCase(settings.copy(sessionDownloadDirectory = settings.defaultDownloadDirectory))
                downloadManager.downloadToDirectoryPath(bucketId, fullPath, item.name, settings.defaultDownloadDirectory)
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
            val manual = _manualState.value
            val item = manual.itemToDownload ?: return@launch
            val settings = getUserSettingsUseCase().firstOrNull() ?: return@launch
            val fullPath = item.getFullPath(path)
            
            updateUserSettingsUseCase(settings.copy(sessionDownloadDirectory = pickedPath))
            downloadManager.downloadToDirectoryPath(bucketId, fullPath, item.name, pickedPath)
            _manualState.update { it.copy(successMessage = "Download started", isPickingDirectory = false, itemToDownload = null) }
        }
    }

    fun onDirectoryPickingCancelled() {
        _manualState.update { it.copy(isPickingDirectory = false, itemToDownload = null) }
    }
    
    fun onFileSaved() {
        _manualState.update { it.copy(isSavingFile = false, itemToDownload = null) }
    }

    fun clearItemToDownload() {
        _manualState.update { it.copy(itemToDownload = null, isSavingFile = false, showDownloadPathOptionDialog = false) }
    }

    fun startDownload(name: String, platformFile: PlatformFile) {
        viewModelScope.launch {
            val fullPath = if (path.isNullOrEmpty()) name else "$path/$name"
            downloadManager.download(
                bucketId = bucketId,
                path = fullPath,
                fileName = name,
                platformFile = platformFile
            )
            _manualState.update { it.copy(successMessage = "Download started", isSavingFile = false, itemToDownload = null) }
        }
    }

    fun clearMessages() {
        _manualState.update { it.copy(error = null, successMessage = null) }
    }

    fun toggleStar(item: StorageItem) {
        viewModelScope.launch {
            val fullPath = item.getFullPath(path)
            val itemId = "$bucketId:$fullPath"
            
            val starredItem = StarredItem(
                id = itemId,
                fileName = item.name,
                bucketId = bucketId,
                path = fullPath,
                isFolder = item.isFolder,
                isBucket = false,
                starredAt = kotlinx.datetime.Clock.System.now()
            )
            
            toggleStarUseCase(starredItem, item.isStarred).fold(
                onSuccess = {
                    val message = if (item.isStarred) "Unstarred successfully" else "Starred successfully"
                    _manualState.update { it.copy(successMessage = message) }
                },
                onFailure = { error ->
                    _manualState.update { it.copy(error = error.message) }
                }
            )
        }
    }
}

data class ManualUiState(
    val itemToDownload: StorageItem? = null,
    val isPickingDirectory: Boolean = false,
    val isSavingFile: Boolean = false,
    val showDownloadPathOptionDialog: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)
