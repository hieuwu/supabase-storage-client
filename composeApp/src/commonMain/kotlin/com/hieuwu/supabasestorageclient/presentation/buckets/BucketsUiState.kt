package com.hieuwu.supabasestorageclient.presentation.buckets

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.ViewMode

sealed interface BucketsUiState {
    object Loading : BucketsUiState
    data class Content(
        val buckets: List<Bucket> = emptyList(),
        val viewMode: ViewMode = ViewMode.LIST,
        val showEmptyConfirmation: Boolean = false,
        val showDeleteConfirmation: Boolean = false,
        val selectedBucket: Bucket? = null,
        val successMessage: String? = null,
        val error: String? = null
    ) : BucketsUiState
    data class Error(val message: String) : BucketsUiState
}