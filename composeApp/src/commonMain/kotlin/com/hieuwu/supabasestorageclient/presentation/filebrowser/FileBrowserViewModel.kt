package com.hieuwu.supabasestorageclient.presentation.filebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.download.DownloadManager
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.MoveFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetPublicUrlUseCase
import com.hieuwu.supabasestorageclient.util.ClipboardManager
import com.hieuwu.supabasestorageclient.util.DirectoryPicker
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.feature.usecase.storage.RefreshBucketContentsUseCase
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BucketUiState(
    val items: List<StorageItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

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
    private val directoryPicker: DirectoryPicker,
    private val starredRepository: StarredRepository,
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
            val result = getBucketContentsUseCase(bucketId, path.orEmpty())
            starredRepository.getStarredItems().collect { stars ->
                result.onSuccess { items ->
                    val starredPaths = stars.filter { it.bucketId == bucketId }.map { it.path ?: "" }.toSet()
                    val updatedItems = items.map { item ->
                        val fullPath = if (path.isNullOrEmpty()) item.name else "$path/${item.name}"
                        item.copy(isStarred = starredPaths.contains(fullPath))
                    }
                    _uiState.value = _uiState.value.copy(items = updatedItems, isLoading = false)
                }.onFailure { error ->
                    logger.e(error) { "Failed to load contents for bucket $bucketId at path $path" }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
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
            moveFileUseCase(bucketId, oldFullPath, newFullPath).fold(
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
            moveFileUseCase(bucketId, oldFullPath, newFullPath).fold(
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
            deleteFileUseCase(bucketId, fullPath).fold(
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
            getPublicUrlUseCase(bucketId, fullPath).fold(
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
        viewModelScope.launch {
            val destDir = directoryPicker.pickDirectory()
            if (destDir == null) return@launch
            val fullPath = if (path.isNullOrEmpty()) name else "$path/$name"
            downloadManager.download(
                bucketId = bucketId,
                path = fullPath,
                fileName = name,
                destinationPath = destDir
            )
            _uiState.update { it.copy(successMessage = "Download started") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun toggleStar(item: StorageItem) {
        viewModelScope.launch {
            val fullPath = if (path.isNullOrEmpty()) item.name else "$path/${item.name}"
            val itemId = "$bucketId:$fullPath"
            if (item.isStarred) {
                starredRepository.unstarItem(itemId)
                _uiState.update { it.copy(successMessage = "Unstarred successfully") }
            } else {
                starredRepository.starItem(
                    StarredItem(
                        itemId = itemId,
                        itemName = item.name,
                        bucketId = bucketId,
                        path = fullPath,
                        isFolder = item.isFolder,
                        isBucket = false,
                        starredAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    )
                )
                _uiState.update { it.copy(successMessage = "Starred successfully") }
            }
        }
    }
}
