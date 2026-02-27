package com.hieuwu.supabasestorageclient.presentation.buckets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.context.ContextSelectionManager
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.EmptyBucketUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.DeleteBucketUseCase
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.feature.usecase.storage.RefreshBucketsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BucketsUiState(
    val buckets: List<Bucket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showEmptyConfirmation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val selectedBucket: Bucket? = null
)

class BucketsViewModel(
    private val getBucketsUseCase: GetBucketsUseCase,
    private val refreshBucketsUseCase: RefreshBucketsUseCase,
    private val emptyBucketUseCase: EmptyBucketUseCase,
    private val deleteBucketUseCase: DeleteBucketUseCase,
    private val contextSelectionManager: ContextSelectionManager,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(BucketsUiState())
    val uiState: StateFlow<BucketsUiState> = _uiState.asStateFlow()

    init {
        contextSelectionManager.clearContext()
        loadBuckets()
    }

    fun loadBuckets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            getBucketsUseCase()
                .onSuccess { buckets ->
                    _uiState.value = _uiState.value.copy(buckets = buckets, isLoading = false)
                }
                .onFailure { error ->
                    logger.e(error) { "Failed to load buckets" }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
        }
    }

    fun refreshBuckets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            refreshBucketsUseCase()
                .onSuccess {
                    loadBuckets()
                }
                .onFailure { error ->
                    logger.e(error) { "Failed to refresh buckets" }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
        }
    }

    fun onEmptyBucketClick(bucket: Bucket) {
        _uiState.value = _uiState.value.copy(selectedBucket = bucket, showEmptyConfirmation = true)
    }

    fun onDeleteBucketClick(bucket: Bucket) {
        _uiState.value = _uiState.value.copy(selectedBucket = bucket, showDeleteConfirmation = true)
    }

    fun confirmEmptyBucket() {
        val bucketId = _uiState.value.selectedBucket?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showEmptyConfirmation = false)
            emptyBucketUseCase(bucketId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Bucket emptied successfully",
                        selectedBucket = null
                    )
                }
                .onFailure { error ->
                    logger.e(error) { "Failed to empty bucket $bucketId" }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to empty bucket: ${error.message}",
                        selectedBucket = null
                    )
                }
        }
    }

    fun confirmDeleteBucket() {
        val bucketId = _uiState.value.selectedBucket?.id ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, showDeleteConfirmation = false)
            deleteBucketUseCase(bucketId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Bucket deleted successfully",
                        selectedBucket = null
                    )
                    loadBuckets()
                }
                .onFailure { error ->
                    logger.e(error) { "Failed to delete bucket $bucketId" }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to delete bucket: ${error.message}",
                        selectedBucket = null
                    )
                }
        }
    }

    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(
            showEmptyConfirmation = false,
            showDeleteConfirmation = false,
            selectedBucket = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, error = null)
    }
}
