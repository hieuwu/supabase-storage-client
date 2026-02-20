package com.hieuwu.supabasestorageclient.presentation.fileview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DeleteFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DownloadFileUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetFileMetadataUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetPublicUrlUseCase
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
    val isDeleted: Boolean = false
)

class FileViewViewModel(
    private val bucketId: String,
    private val fileName: String,
    private val path: String?,
    private val getPublicUrlUseCase: GetPublicUrlUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val getFileMetadataUseCase: GetFileMetadataUseCase
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
            
            // Load Public URL
            getPublicUrlUseCase(bucketId, fullPath).fold(
                onSuccess = { url ->
                    _uiState.update { it.copy(publicUrl = url) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            )

            // Load Metadata
            getFileMetadataUseCase(bucketId, fullPath).fold(
                onSuccess = { metadata ->
                    _uiState.update { it.copy(metadata = metadata, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun downloadFile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            downloadFileUseCase(bucketId, fullPath).fold(
                onSuccess = { 
                    // In a real app, we'd save this to disk.
                    // For now, we just notify success or failure.
                    _uiState.update { it.copy(isLoading = false) }
                 },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun deleteFile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fullPath = if (path.isNullOrEmpty()) fileName else "$path/$fileName"
            deleteFileUseCase(bucketId, fullPath).fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeleted = true, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }
}
