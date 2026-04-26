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
    val size: Long? = null,
    val isStarred: Boolean = false
) {
    fun getFullPath(parentPath: String?): String {
        return if (parentPath.isNullOrEmpty()) name else "$parentPath/$name"
    }

    val extension: String
        get() = if (isFolder) "" else name.substringAfterLast(".", "")

    val isImage: Boolean
        get() = !isFolder && extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    val isVideo: Boolean
        get() = !isFolder && extension.lowercase() in listOf("mp4", "mov", "avi", "mkv", "webm")
}
