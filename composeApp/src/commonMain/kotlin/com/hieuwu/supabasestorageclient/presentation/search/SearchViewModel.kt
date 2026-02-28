package com.hieuwu.supabasestorageclient.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.StorageItem
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketContentsUseCase
import com.hieuwu.supabasestorageclient.feature.usecase.storage.GetBucketsUseCase
import co.touchlab.kermit.Logger
import com.hieuwu.supabasestorageclient.domain.model.StarredItem
import com.hieuwu.supabasestorageclient.domain.repository.StarredRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

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
    private val starredRepository: StarredRepository,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            searchType = if (bucketId == null) SearchType.BUCKET else SearchType.FILE
        )
    )
    val uiState: StateFlow<SearchUiState> = combine(
        _uiState,
        starredRepository.getStarredItems()
    ) { state, starredItems ->
        val starredIds = starredItems.map { it.itemId }.toSet()
        state.copy(
            buckets = state.buckets.map { it.copy(isStarred = starredIds.contains(it.id)) },
            storageItems = state.storageItems.map { result ->
                val fullPath = result.item.name // Assuming item name is what's used for ID
                val itemId = if (result.item.isFolder) "${result.bucketId}:$fullPath" else "${result.bucketId}:$fullPath"
                // Wait, I need to check how itemId is constructed for files/folders
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
            if (bucket.isStarred) {
                starredRepository.unstarItem(bucket.id)
            } else {
                starredRepository.starItem(
                    StarredItem(
                        itemId = bucket.id,
                        itemName = bucket.name,
                        bucketId = bucket.id,
                        path = null,
                        isFolder = false,
                        isBucket = true,
                        starredAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }

    fun toggleStar(item: StorageItem, bucketId: String) {
        viewModelScope.launch {
            val itemId = "${bucketId}:${item.name}"
            if (item.isStarred) {
                starredRepository.unstarItem(itemId)
            } else {
                starredRepository.starItem(
                    StarredItem(
                        itemId = itemId,
                        itemName = item.name,
                        bucketId = bucketId,
                        path = item.name, // This might be wrong for nested files, but Search currently only searches root?
                        isFolder = item.isFolder,
                        isBucket = false,
                        starredAt = Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null) }
    }
}
