package com.hieuwu.supabasestorageclient.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.feature.usecase.storage.CreateFolderUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.UploadFileUseCase
import com.hieuwu.supabasestorageclient.util.FilePicker
import com.hieuwu.supabasestorageclient.util.PermissionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isNewFolderDialogVisible: Boolean = false,
    val newFolderName: String = "",
    val error: String? = null,
    val successMessage: String? = null,
    val isUploading: Boolean = false
)

class MainViewModel(
    private val contextSelectionManager: ContextSelectionManager,
    private val createFolderUseCase: CreateFolderUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val filePicker: FilePicker,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigateToUploads = MutableSharedFlow<Unit>()
    val navigateToUploads: SharedFlow<Unit> = _navigateToUploads.asSharedFlow()

    fun onNewFolderClick() {
        _uiState.update { it.copy(isNewFolderDialogVisible = true) }
    }

    fun onDismissNewFolderDialog() {
        _uiState.update { it.copy(isNewFolderDialogVisible = false, newFolderName = "") }
    }

    fun onNewFolderNameChange(name: String) {
        _uiState.update { it.copy(newFolderName = name) }
    }

    fun onCreateFolder() {
        val folderName = _uiState.value.newFolderName
        if (folderName.isBlank()) return

        val context = contextSelectionManager.currentContext.value
        if (context == null) {
            _uiState.update { it.copy(error = "Please select a bucket first") }
            return
        }

        viewModelScope.launch {
            val fullPath = if (context.path.isEmpty()) folderName else "${context.path}/$folderName"
            createFolderUseCase(context.bucketId, fullPath).fold(
                onSuccess = {
                    _uiState.update { it.copy(isNewFolderDialogVisible = false, newFolderName = "", successMessage = "Folder created") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )
        }
    }

    fun onUploadFileClick() {
        viewModelScope.launch {
            if (!permissionManager.requestStoragePermission()) {
                _uiState.update { it.copy(error = "Permission denied") }
                return@launch
            }

            val context = contextSelectionManager.currentContext.value
            if (context == null) {
                _uiState.update { it.copy(error = "Please select a bucket first") }
                return@launch
            }

            val selectedFile = filePicker.pickFile()
            if (selectedFile != null) {
                val fullPath = if (context.path.isEmpty()) selectedFile.name else "${context.path}/${selectedFile.name}"
                uploadFileUseCase(context.bucketId, fullPath, selectedFile.name, selectedFile.data)
                _uiState.update { it.copy(successMessage = "Upload started", isUploading = true) }
                _navigateToUploads.emit(Unit)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null, isUploading = false) }
    }
}
