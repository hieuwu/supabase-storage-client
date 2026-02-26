package com.hieuwu.supabasestorageclient.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketsUseCase
import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class SearchType {
    BUCKET, FOLDER, FILE
}

data class SearchResult(
    val item: StorageItem,
    val bucketId: String
)

data class SearchUiState(
    val query: String = "",
    val searchType: SearchType = SearchType.BUCKET,
    val isLoading: Boolean = false,
    val buckets: List<Bucket> = emptyList(),
    val storageItems: List<SearchResult> = emptyList(),
    val error: String? = null
)

class SearchViewModel(
    private val bucketId: String?,
    private val getBucketsUseCase: GetBucketsUseCase,
    private val getBucketContentsUseCase: GetBucketContentsUseCase,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            searchType = if (bucketId == null) SearchType.BUCKET else SearchType.FILE
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        _uiState.update { it.copy(buckets = emptyList(), storageItems = emptyList(), isLoading = false) }
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        queryFlow.value = newQuery
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (bucketId == null) {
                    // Search for buckets
                    getBucketsUseCase().onSuccess { buckets ->
                        val filtered = buckets.filter { it.name.contains(query, ignoreCase = true) }
                        _uiState.update {
                            it.copy(
                                buckets = filtered,
                                storageItems = emptyList(),
                                isLoading = false,
                                searchType = SearchType.BUCKET
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                } else {
                    // Search for files and folders in the specific bucket
                    val items = getBucketContentsUseCase(bucketId, "").getOrNull() ?: emptyList()
                    val filtered = items.filter { it.name.contains(query, ignoreCase = true) }
                        .map { SearchResult(it, bucketId) }
                    _uiState.update {
                        it.copy(
                            storageItems = filtered,
                            buckets = emptyList(),
                            isLoading = false,
                            searchType = SearchType.FILE // This can be refined to show both files and folders
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Error performing search" }
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null) }
    }
}
