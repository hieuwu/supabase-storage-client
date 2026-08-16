package com.hieuwu.supabasestorageclient.domain.error

enum class StorageOperation(val label: String) {
    Upload("Upload"),
    Download("Download")
}

/**
 * Turns the raw exception from the storage API / file system into a single short message
 * that says why the operation failed.
 */
fun storageErrorMessage(error: Throwable, operation: StorageOperation): String {
    val details = buildString {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < 5) {
            current.message?.let { append(it).append(' ') }
            current::class.simpleName?.let { append(it).append(' ') }
            current = current.cause
            depth++
        }
    }.lowercase()

    val reason = when {
        details.contains("row-level security") ||
            details.contains("row level security") ||
            details.contains("new row violates") ->
            "the bucket's row level security policy blocked it"

        details.contains("jwt") ||
            details.contains("invalid api key") ||
            details.contains("invalid token") ||
            details.contains("invalid claim") ->
            "the API key is invalid or expired"

        details.contains("unauthorized") || details.contains("401") ->
            "your key is not authorized for this bucket"

        details.contains("403") || details.contains("forbidden") || details.contains("access denied") ->
            "access was denied - the bucket is missing a policy for this action"

        details.contains("bucket not found") ->
            "the bucket no longer exists"

        details.contains("not found") || details.contains("404") ->
            "the file was not found on the server"

        details.contains("already exists") || details.contains("duplicate") ->
            "a file with the same name already exists in the bucket"

        details.contains("413") ||
            details.contains("payload too large") ||
            details.contains("maximum allowed size") ->
            "the file is bigger than the bucket's size limit"

        details.contains("mime type") ->
            "the file type is not allowed by the bucket"

        details.contains("enoent") ||
            details.contains("no such file") ||
            details.contains("filenotfound") ->
            "the destination folder is not available - pick the download folder again"

        details.contains("eacces") ||
            details.contains("securityexception") ||
            details.contains("permission") ->
            "there is no permission to write to the selected folder"

        details.contains("enospc") || details.contains("no space left") ->
            "there is not enough free space on the device"

        details.contains("unknownhost") ||
            details.contains("unresolvedaddress") ||
            details.contains("connect") ||
            details.contains("timeout") ||
            details.contains("timed out") ->
            "the server could not be reached - check your connection"

        else -> error.message ?: "an unexpected error occurred"
    }

    return "${operation.label} failed because $reason"
}
