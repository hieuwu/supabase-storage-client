package com.hieuwu.supabasestorageclient.observability.analytics

import com.hieuwu.supabasestorageclient.domain.error.StorageErrorReason

/**
 * Moving files — the product's core loop, and the only thing here whose failure is otherwise
 * invisible outside the user's own screen.
 *
 * These keep dedicated event names rather than folding into [StorageEvents.STORAGE_ACTION] because
 * they are long-running and carry dimensions nothing else does: size, duration, and where the file
 * was going. `network` failures clustered in a large [AnalyticsParams.SIZE_BUCKET] with a long
 * [AnalyticsParams.DURATION_MS] read as a resumable-transfer gap, not a bug.
 */
object TransferEvents {
    const val FILE_UPLOAD_STARTED = "file_upload_started"
    const val FILE_UPLOAD_SUCCEEDED = "file_upload_succeeded"
    const val FILE_UPLOAD_FAILED = "file_upload_failed"
    const val FILE_UPLOAD_CANCELLED = "file_upload_cancelled"

    const val FILE_DOWNLOAD_STARTED = "file_download_started"
    const val FILE_DOWNLOAD_SUCCEEDED = "file_download_succeeded"
    const val FILE_DOWNLOAD_FAILED = "file_download_failed"
    const val FILE_DOWNLOAD_CANCELLED = "file_download_cancelled"
}

/**
 * Where a download was written. Values for [AnalyticsParams.DESTINATION_MODE], and the way to tell
 * whether the "ask every time / once per session / never" setting is pulling its weight.
 */
object DownloadDestinationModes {
    const val DEFAULT_FOLDER = "default_folder"
    const val SESSION_FOLDER = "session_folder"
    const val PICKED_FOLDER = "picked_folder"
    const val PICKED_FILE = "picked_file"
}

/** [fileName] is used only to derive the extension; it is never sent. */
fun AppAnalytics.logUploadStarted(fileName: String, sizeBytes: Long) {
    logEvent(
        TransferEvents.FILE_UPLOAD_STARTED,
        mapOf(
            AnalyticsParams.FILE_EXTENSION to fileExtensionOf(fileName),
            AnalyticsParams.SIZE_BYTES to sizeBytes,
            AnalyticsParams.SIZE_BUCKET to sizeBucketOf(sizeBytes),
        ),
    )
}

fun AppAnalytics.logUploadSucceeded(fileName: String, sizeBytes: Long, durationMs: Long) {
    logEvent(
        TransferEvents.FILE_UPLOAD_SUCCEEDED,
        mapOf(
            AnalyticsParams.FILE_EXTENSION to fileExtensionOf(fileName),
            AnalyticsParams.SIZE_BYTES to sizeBytes,
            AnalyticsParams.SIZE_BUCKET to sizeBucketOf(sizeBytes),
            AnalyticsParams.DURATION_MS to durationMs,
        ),
    )
}

fun AppAnalytics.logUploadFailed(fileName: String, sizeBytes: Long, reason: StorageErrorReason) {
    logEvent(
        TransferEvents.FILE_UPLOAD_FAILED,
        mapOf(
            AnalyticsParams.FILE_EXTENSION to fileExtensionOf(fileName),
            AnalyticsParams.SIZE_BUCKET to sizeBucketOf(sizeBytes),
            AnalyticsParams.REASON to reason.key,
        ),
    )
}

fun AppAnalytics.logUploadCancelled() {
    logEvent(TransferEvents.FILE_UPLOAD_CANCELLED)
}

/** [destinationMode] is one of [DownloadDestinationModes]. */
fun AppAnalytics.logDownloadStarted(fileName: String, destinationMode: String) {
    logEvent(
        TransferEvents.FILE_DOWNLOAD_STARTED,
        mapOf(
            AnalyticsParams.FILE_EXTENSION to fileExtensionOf(fileName),
            AnalyticsParams.DESTINATION_MODE to destinationMode,
        ),
    )
}

fun AppAnalytics.logDownloadSucceeded(fileName: String, sizeBytes: Long, durationMs: Long) {
    logEvent(
        TransferEvents.FILE_DOWNLOAD_SUCCEEDED,
        mapOf(
            AnalyticsParams.FILE_EXTENSION to fileExtensionOf(fileName),
            AnalyticsParams.SIZE_BYTES to sizeBytes,
            AnalyticsParams.SIZE_BUCKET to sizeBucketOf(sizeBytes),
            AnalyticsParams.DURATION_MS to durationMs,
        ),
    )
}

fun AppAnalytics.logDownloadFailed(fileName: String, reason: StorageErrorReason) {
    logEvent(
        TransferEvents.FILE_DOWNLOAD_FAILED,
        mapOf(
            AnalyticsParams.FILE_EXTENSION to fileExtensionOf(fileName),
            AnalyticsParams.REASON to reason.key,
        ),
    )
}

fun AppAnalytics.logDownloadCancelled() {
    logEvent(TransferEvents.FILE_DOWNLOAD_CANCELLED)
}
