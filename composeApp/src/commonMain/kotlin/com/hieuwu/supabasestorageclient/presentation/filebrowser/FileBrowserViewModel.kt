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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.hieuwu.supabasestorageclient.domain.model.AskDownloadPathConfig
import com.hieuwu.supabasestorageclient.util.ClipboardManager
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
    private val contextSelectionManager: ContextSelectionManager,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(BucketUiState())
    val uiState: StateFlow<BucketUiState> = _uiState.asStateFlow()

    init {
        contextSelectionManager.setContext(bucketId, path ?: "")
        loadContents()
    }

    fun loadContents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val params = GetBucketContentsUseCase.Params(bucketId = bucketId, path = path.orEmpty())
            val result = getBucketContentsUseCase(params)
            val starredItems = getStarredItemsUseCase()
            val userSettings = getUserSettingsUseCase()

            combine(starredItems, userSettings) { stars, settings ->
                Triple(stars, settings.viewMode, settings.defaultDownloadDirectory)
            }.collect { (stars, viewMode, defaultPath) ->
                result.onSuccess { items ->
                    val starredPaths =
                        stars.filter { it.bucketId == bucketId }.map { it.path ?: "" }.toSet()
                    val updatedItems = items.map { item ->
                        item.copy(isStarred = starredPaths.contains(item.getFullPath(path)))
                    }
                    _uiState.value =
                        _uiState.value.copy(
                            items = updatedItems,
                            isLoading = false,
                            viewMode = viewMode,
                            defaultDownloadPath = defaultPath
                        )
                }.onFailure { error ->
                    logger.e(error) { "Failed to load contents for bucket $bucketId at path $path" }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message,
                        viewMode = viewMode,
                        defaultDownloadPath = defaultPath
                    )
                }
            }
        }
    }

    fun refreshContents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            refreshBucketContentsUseCase(bucketId)
                .onSuccess {
                    loadContents()
                }
                .onFailure { error ->
                    logger.e(error) { "Failed to refresh contents for bucket $bucketId" }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
        }
    }

    fun renameItem(oldName: String, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val oldFullPath = if (path.isNullOrEmpty()) oldName else "$path/$oldName"
            val newFullPath = if (path.isNullOrEmpty()) newName else "$path/$newName"
            val params = MoveFileUseCase.Params(bucketId = bucketId, fromPath = oldFullPath, toPath = newFullPath)
            moveFileUseCase(params).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Renamed successfully", isLoading = false) }
                    loadContents()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun moveItem(oldName: String, newPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val oldFullPath = if (path.isNullOrEmpty()) oldName else "$path/$oldName"
            val newFullPath = if (newPath.isEmpty()) oldName else "$newPath/$oldName"
            val params = MoveFileUseCase.Params(bucketId = bucketId, fromPath = oldFullPath, toPath = newFullPath)
            moveFileUseCase(params).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Moved successfully", isLoading = false) }
                    loadContents()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun deleteItem(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fullPath = if (path.isNullOrEmpty()) name else "$path/$name"
            val params = DeleteFileUseCase.Params(bucketId = bucketId, path = fullPath)
            deleteFileUseCase(params).fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Deleted successfully", isLoading = false) }
                    loadContents()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
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
                    _uiState.update { it.copy(successMessage = "URL copied to clipboard") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun copyPath(name: String) {
        val fullPath = if (path.isNullOrEmpty()) name else "$path/$name"
        clipboardManager.copyText(fullPath)
        _uiState.update { it.copy(successMessage = "Path copied to clipboard") }
    }

    fun downloadItem(name: String) {
        val item = _uiState.value.items.find { it.name == name } ?: return
        viewModelScope.launch {
            val settings = getUserSettingsUseCase().firstOrNull() ?: return@launch
            val fullPath = item.getFullPath(path)

            when (settings.askDownloadPathConfig) {
                AskDownloadPathConfig.NEVER_ASK -> {
                    if (settings.defaultDownloadDirectory != null) {
                        downloadManager.downloadToDirectoryPath(bucketId, fullPath, name, settings.defaultDownloadDirectory)
                        _uiState.update { it.copy(successMessage = "Download started") }
                    } else {
                        _uiState.update { it.copy(itemToDownload = item, isSavingFile = true) }
                    }
                }
                AskDownloadPathConfig.ONCE_WHEN_APP_OPEN -> {
                    if (settings.sessionDownloadDirectory != null) {
                        downloadManager.downloadToDirectoryPath(bucketId, fullPath, name, settings.sessionDownloadDirectory)
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
                    _uiState.update { it.copy(
                        showDownloadPathOptionDialog = true,
                        itemToDownload = item,
                        defaultDownloadPath = settings.defaultDownloadDirectory
                    ) }
                }
            }
        }
    }

    fun onSelectDefaultPath() {
        val item = _uiState.value.itemToDownload ?: return
        viewModelScope.launch {
            val settings = getUserSettingsUseCase().firstOrNull() ?: return@launch
            val fullPath = item.getFullPath(path)
            
            if (settings.defaultDownloadDirectory != null) {
                // In a perfect DDD, we'd have a UseCase for this settings update too
                // For now, let's just use the downloadManager and maybe add UpdateSettingsUseCase later
                downloadManager.downloadToDirectoryPath(bucketId, fullPath, item.name, settings.defaultDownloadDirectory)
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
            val item = _uiState.value.itemToDownload ?: return@launch
            val fullPath = item.getFullPath(path)
            
            // Note: Settings update should ideally be via UpdateUserSettingsUseCase
            downloadManager.downloadToDirectoryPath(bucketId, fullPath, item.name, pickedPath)
            _uiState.update { it.copy(successMessage = "Download started", isPickingDirectory = false, itemToDownload = null) }
        }
    }

    fun onDirectoryPickingCancelled() {
        _uiState.update { it.copy(isPickingDirectory = false, itemToDownload = null) }
    }
    
    fun onFileSaved() {
        _uiState.update { it.copy(isSavingFile = false, itemToDownload = null) }
    }

    fun clearItemToDownload() {
        _uiState.update { it.copy(itemToDownload = null, isSavingFile = false, showDownloadPathOptionDialog = false) }
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
            _uiState.update { it.copy(successMessage = "Download started", isSavingFile = false, itemToDownload = null) }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
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
                    _uiState.update { it.copy(successMessage = message) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }
}
