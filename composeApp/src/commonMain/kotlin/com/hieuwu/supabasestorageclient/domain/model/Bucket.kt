package com.hieuwu.supabasestorageclient.domain.model

data class Bucket(
    val id: String,
    val name: String,
    val owner: String,
    val public: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val allowedMimeTypes: List<String>?,
    val fileSizeLimit: Long?,
    val isStarred: Boolean = false
) {
    fun isMimeTypeAllowed(mimeType: String): Boolean {
        if (allowedMimeTypes == null) return true
        return allowedMimeTypes.contains(mimeType)
    }
}
