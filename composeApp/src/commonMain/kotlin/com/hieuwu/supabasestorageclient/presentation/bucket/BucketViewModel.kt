package com.hieuwu.supabasestorageclient.presentation.bucket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.MoveFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetPublicUrlUseCase
import com.hieuwu.supabasestorageclient.util.ClipboardManager
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

class BucketViewModel(
    private val bucketId: String,
    private val path: String?,
    private val getBucketContentsUseCase: GetBucketContentsUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val moveFileUseCase: MoveFileUseCase,
    private val getPublicUrlUseCase: GetPublicUrlUseCase,
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BucketUiState())
    val uiState: StateFlow<BucketUiState> = _uiState.asStateFlow()

    init {
        loadContents()
    }

    fun loadContents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            getBucketContentsUseCase(bucketId, path.orEmpty())
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(items = items, isLoading = false)
                }
                .onFailure { error ->
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

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
