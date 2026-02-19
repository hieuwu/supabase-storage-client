package com.hieuwu.supabasestorageclient.presentation.buckets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BucketsUiState(
    val buckets: List<Bucket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class BucketsViewModel(
    private val getBucketsUseCase: GetBucketsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BucketsUiState())
    val uiState: StateFlow<BucketsUiState> = _uiState.asStateFlow()

    init {
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
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
        }
    }
}
