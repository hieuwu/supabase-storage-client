package com.hieuwu.supabasestorageclient.domain.model

data class StarredItem(
    val itemId: String,
    val itemName: String,
    val bucketId: String,
    val path: String?,
    val isFolder: Boolean,
    val isBucket: Boolean,
    val starredAt: Long
)
