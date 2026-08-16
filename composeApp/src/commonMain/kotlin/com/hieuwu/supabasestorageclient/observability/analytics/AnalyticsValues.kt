package com.hieuwu.supabasestorageclient.observability.analytics

/**
 * The shaping every raw value passes through before it can become a parameter.
 *
 * Two jobs, and they point the same way: keep user data out of the payload, and keep cardinality
 * low enough that the console can group by these at all.
 */

/**
 * The extension of [fileName], lowercased, or `"none"` / `"other"`.
 *
 * Deliberately lossy: this is the only part of a file name that ever leaves the device, and the
 * length/charset cap stops an odd name from turning into a high-cardinality label.
 */
internal fun fileExtensionOf(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when {
        extension.isEmpty() || extension == fileName.lowercase() -> "none"
        extension.length > 8 || extension.any { it !in 'a'..'z' && it !in '0'..'9' } -> "other"
        else -> extension
    }
}

/** Coarse size band, so the console can segment without exposing exact file sizes per event. */
internal fun sizeBucketOf(sizeBytes: Long): String = when {
    sizeBytes <= 0L -> "unknown"
    sizeBytes < 1_048_576L -> "under_1mb"
    sizeBytes < 10L * 1_048_576L -> "1_10mb"
    sizeBytes < 100L * 1_048_576L -> "10_100mb"
    sizeBytes < 1_073_741_824L -> "100mb_1gb"
    else -> "over_1gb"
}

/** Counts become bands before they become user properties — 36-char cap, and trends read better. */
internal fun countBucketOf(count: Int): String = when {
    count <= 0 -> "0"
    count == 1 -> "1"
    count == 2 -> "2"
    count <= 5 -> "3_5"
    count <= 20 -> "6_20"
    else -> "21_plus"
}
