package com.hieuwu.supabasestorageclient.presentation.buckets

import com.hieuwu.supabasestorageclient.domain.model.Bucket
import com.hieuwu.supabasestorageclient.domain.model.ViewMode

data class BucketsUiState(
    val buckets: List<Bucket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showEmptyConfirmation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val selectedBucket: Bucket? = null,
    val viewMode: ViewMode = ViewMode.LIST
)