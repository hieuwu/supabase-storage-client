package com.hieuwu.supabasestorageclient.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.domain.usecase.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.GetBucketsUseCase
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.usecase.GetStarredItemsUseCase
import com.hieuwu.supabasestorageclient.domain.usecase.ToggleStarUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

class SearchViewModel(
    private val bucketId: String?,
    private val getBucketsUseCase: GetBucketsUseCase,
    private val getBucketContentsUseCase: GetBucketContentsUseCase,
    private val getStarredItemsUseCase: GetStarredItemsUseCase,
    private val toggleStarUseCase: ToggleStarUseCase,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            searchType = if (bucketId == null) SearchType.BUCKET else SearchType.FILE
        )
    )
    val uiState: StateFlow<SearchUiState> = combine(
        _uiState,
        getStarredItemsUseCase()
    ) { state, starredItems ->
        val starredIds = starredItems.map { it.id }.toSet()
        state.copy(
            buckets = state.buckets.map { it.copy(isStarred = starredIds.contains(it.id)) },
            storageItems = state.storageItems.map { result ->
                val fullPath = result.item.name
                val itemId = "${result.bucketId}:$fullPath"
                result.copy(item = result.item.copy(isStarred = starredIds.contains(itemId) || starredIds.contains("${result.bucketId}:${result.item.name}")))
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _uiState.value)

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
                    val params = GetBucketContentsUseCase.Params(bucketId = bucketId, path = "")
                    val items = getBucketContentsUseCase(params).getOrNull() ?: emptyList()
                    val filtered = items.filter { it.name.contains(query, ignoreCase = true) }
                        .map { SearchResult(it, bucketId) }
                    _uiState.update {
                        it.copy(
                            storageItems = filtered,
                            buckets = emptyList(),
                            isLoading = false,
                            searchType = SearchType.FILE
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Error performing search" }
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleStar(bucket: Bucket) {
        viewModelScope.launch {
            toggleStarUseCase(
                StarredItem(
                    id = bucket.id,
                    fileName = bucket.name,
                    bucketId = bucket.id,
                    path = null,
                    isFolder = false,
                    isBucket = true,
                    starredAt = Clock.System.now()
                ),
                isStarred = bucket.isStarred
            )
        }
    }

    fun toggleStar(item: StorageItem, bucketId: String) {
        viewModelScope.launch {
            val itemId = "${bucketId}:${item.name}"
            toggleStarUseCase(
                StarredItem(
                    id = itemId,
                    fileName = item.name,
                    bucketId = bucketId,
                    path = item.name,
                    isFolder = item.isFolder,
                    isBucket = false,
                    starredAt = Clock.System.now(),
                ),
                item.isStarred
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null) }
    }
}
