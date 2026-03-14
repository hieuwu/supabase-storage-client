package com.hieuwu.supabasestorageclient.domain.model

import kotlinx.datetime.Instant

data class StarredItem(
    val id: String,
    val fileName: String,
    val bucketId: String,
    val path: String?,
    val isFolder: Boolean,
    val isBucket: Boolean,
    val starredAt: Instant
)
