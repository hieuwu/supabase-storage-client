package com.hieuwu.supabasestorageclient.presentation.bucket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketContentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BucketUiState(
    val items: List<StorageItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BucketViewModel(
    private val bucketId: String,
    private val path: String?,
    private val getBucketContentsUseCase: GetBucketContentsUseCase
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
}
