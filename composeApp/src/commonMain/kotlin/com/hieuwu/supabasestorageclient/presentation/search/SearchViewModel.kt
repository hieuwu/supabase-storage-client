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
    private val getBucketsUseCase: GetBucketsUseCase,
    private val getBucketContentsUseCase: GetBucketContentsUseCase,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
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
                        performSearch(query, _uiState.value.searchType)
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

    fun onSearchTypeChange(newType: SearchType) {
        _uiState.update { it.copy(searchType = newType) }
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query, newType)
        }
    }

    private fun performSearch(query: String, type: SearchType) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                when (type) {
                    SearchType.BUCKET -> {
                        getBucketsUseCase().onSuccess { buckets ->
                            val filtered = buckets.filter { it.name.contains(query, ignoreCase = true) }
                            _uiState.update { it.copy(buckets = filtered, storageItems = emptyList(), isLoading = false) }
                        }.onFailure { e ->
                            _uiState.update { it.copy(isLoading = false, error = e.message) }
                        }
                    }
                    SearchType.FOLDER, SearchType.FILE -> {
                        getBucketsUseCase().onSuccess { buckets ->
                            val allResults = mutableListOf<SearchResult>()
                            val jobs = buckets.map { bucket ->
                                async {
                                    val items = getBucketContentsUseCase(bucket.id, "").getOrNull() ?: emptyList()
                                    items.map { SearchResult(it, bucket.id) }
                                }
                            }
                            val results = awaitAll(*jobs.toTypedArray())
                            results.forEach { allResults.addAll(it) }

                            val filtered = allResults.filter { result ->
                                val nameMatches = result.item.name.contains(query, ignoreCase = true)
                                val typeMatches = if (type == SearchType.FOLDER) result.item.isFolder else !result.item.isFolder
                                nameMatches && typeMatches
                            }
                            _uiState.update { it.copy(storageItems = filtered, buckets = emptyList(), isLoading = false) }
                        }.onFailure { e ->
                            _uiState.update { it.copy(isLoading = false, error = e.message) }
                        }
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
