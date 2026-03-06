package com.hieuwu.supabasestorageclient.presentation.search

import com.hieuwu.supabasestorageclient.domain.model.StorageItem

data class SearchResult(
    val item: StorageItem,
    val bucketId: String
)