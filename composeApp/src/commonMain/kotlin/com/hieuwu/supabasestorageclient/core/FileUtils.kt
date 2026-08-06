package com.hieuwu.supabasestorageclient.core

object FileUtils {
    fun isImage(extension: String): Boolean =
        extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    fun isVideo(extension: String): Boolean =
        extension in listOf("mp4", "mov", "avi", "mkv", "webm")

    fun isPdf(extension: String): Boolean =
        extension == "pdf"
}