package com.hieuwu.supabasestorageclient.domain.error

enum class StorageOperation(val label: String) {
    Upload("Upload"),
    Download("Download")
}

/**
 * Why a storage call failed, as a stable low-cardinality token.
 *
 * One classification, two consumers: [key] is what analytics reports (raw exception text is
 * unbounded and often contains an object path), [message] is what the user reads.
 */
enum class StorageErrorReason(val key: String, val message: String) {
    RlsPolicy("rls_policy", "the bucket's row level security policy blocked it"),
    InvalidKey("invalid_key", "the API key is invalid or expired"),
    Unauthorized("unauthorized", "your key is not authorized for this bucket"),
    Forbidden("forbidden", "access was denied - the bucket is missing a policy for this action"),
    BucketNotFound("bucket_not_found", "the bucket no longer exists"),
    NotFound("not_found", "the file was not found on the server"),
    AlreadyExists("already_exists", "a file with the same name already exists in the bucket"),
    TooLarge("too_large", "the file is bigger than the bucket's size limit"),
    MimeTypeRejected("mime_type_rejected", "the file type is not allowed by the bucket"),
    DestinationMissing(
        "destination_missing",
        "the destination folder is not available - pick the download folder again",
    ),
    NoWritePermission("no_write_permission", "there is no permission to write to the selected folder"),
    NoSpace("no_space", "there is not enough free space on the device"),
    Network("network", "the server could not be reached - check your connection"),
    Unknown("unknown", "an unexpected error occurred"),
}

/**
 * Classifies [error] by matching the flattened cause chain, which is where the storage API and the
 * platform file system both put the useful part.
 */
fun storageErrorReason(error: Throwable): StorageErrorReason {
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

    return when {
        details.contains("row-level security") ||
            details.contains("row level security") ||
            details.contains("new row violates") -> StorageErrorReason.RlsPolicy

        details.contains("jwt") ||
            details.contains("invalid api key") ||
            details.contains("invalid token") ||
            details.contains("invalid claim") -> StorageErrorReason.InvalidKey

        details.contains("unauthorized") || details.contains("401") -> StorageErrorReason.Unauthorized

        details.contains("403") ||
            details.contains("forbidden") ||
            details.contains("access denied") -> StorageErrorReason.Forbidden

        details.contains("bucket not found") -> StorageErrorReason.BucketNotFound

        details.contains("not found") || details.contains("404") -> StorageErrorReason.NotFound

        details.contains("already exists") || details.contains("duplicate") -> StorageErrorReason.AlreadyExists

        details.contains("413") ||
            details.contains("payload too large") ||
            details.contains("maximum allowed size") -> StorageErrorReason.TooLarge

        details.contains("mime type") -> StorageErrorReason.MimeTypeRejected

        details.contains("enoent") ||
            details.contains("no such file") ||
            details.contains("filenotfound") -> StorageErrorReason.DestinationMissing

        details.contains("eacces") ||
            details.contains("securityexception") ||
            details.contains("permission") -> StorageErrorReason.NoWritePermission

        details.contains("enospc") || details.contains("no space left") -> StorageErrorReason.NoSpace

        details.contains("unknownhost") ||
            details.contains("unresolvedaddress") ||
            details.contains("connect") ||
            details.contains("timeout") ||
            details.contains("timed out") -> StorageErrorReason.Network

        else -> StorageErrorReason.Unknown
    }
}

/**
 * Turns the raw exception from the storage API / file system into a single short message
 * that says why the operation failed.
 */
fun storageErrorMessage(error: Throwable, operation: StorageOperation): String {
    val reason = storageErrorReason(error)
    // An unclassified error still carries its own message, which beats a generic sentence.
    val explanation = if (reason == StorageErrorReason.Unknown) {
        error.message ?: reason.message
    } else {
        reason.message
    }
    return "${operation.label} failed because $explanation"
}
