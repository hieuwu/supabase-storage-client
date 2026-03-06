package com.hieuwu.supabasestorageclient.presentation.search

import com.hieuwu.supabasestorageclient.domain.model.Bucket

data class SearchUiState(
    val query: String = "",
    val searchType: SearchType = SearchType.BUCKET,
    val isLoading: Boolean = false,
    val buckets: List<Bucket> = emptyList(),
    val storageItems: List<SearchResult> = emptyList(),
    val error: String? = null
)