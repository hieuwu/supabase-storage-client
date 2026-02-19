package com.hieuwu.supabasestorageclient.domain.model

import kotlinx.datetime.Instant

data class StorageItem(
    val name: String,
    val id: String?,
    val updatedAt: Instant?,
    val createdAt: Instant?,
    val lastAccessedAt: Instant?,
    val metadata: Map<String, Any>?,
    val isFolder: Boolean,
    val size: Long? = null
)
