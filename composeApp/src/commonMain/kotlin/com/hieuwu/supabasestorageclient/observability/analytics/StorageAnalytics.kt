package com.hieuwu.supabasestorageclient.observability.analytics

import com.hieuwu.supabasestorageclient.domain.error.storageErrorReason

object StorageEvents {
    /**
     * Every one-shot storage mutation, distinguished by [AnalyticsParams.ACTION].
     *
     * These share one name on purpose: individually they are low-volume, they are always read as a
     * group ("is anything I do to a bucket failing?"), and eight more names would crowd the console
     * for no extra question answered. Uploads and downloads keep dedicated names — see
     * [TransferEvents].
     */
    const val STORAGE_ACTION = "storage_action"
}

/** Values for [AnalyticsParams.ACTION] on [StorageEvents.STORAGE_ACTION]. */
object StorageActions {
    const val CREATE_BUCKET = "create_bucket"
    const val UPDATE_BUCKET = "update_bucket"
    const val DELETE_BUCKET = "delete_bucket"
    const val EMPTY_BUCKET = "empty_bucket"
    const val CREATE_FOLDER = "create_folder"
    const val DELETE_FILE = "delete_file"
    const val RENAME_FILE = "rename_file"
    const val MOVE_FILE = "move_file"
}

/** [action] is one of [StorageActions]; [error] is null on success. */
fun AppAnalytics.logStorageAction(
    action: String,
    error: Throwable? = null,
    extras: Map<String, Any?> = emptyMap(),
) {
    logEvent(
        StorageEvents.STORAGE_ACTION,
        buildMap {
            put(AnalyticsParams.ACTION, action)
            put(AnalyticsParams.SUCCESS, error == null)
            if (error != null) put(AnalyticsParams.REASON, storageErrorReason(error).key)
            putAll(extras)
        },
    )
}

/** Bucket creation carries the two choices that shape how people use Supabase storage. */
fun AppAnalytics.logBucketCreated(
    isPublic: Boolean,
    hasSizeLimit: Boolean,
    error: Throwable? = null,
) {
    logStorageAction(
        StorageActions.CREATE_BUCKET,
        error,
        mapOf(
            AnalyticsParams.IS_PUBLIC to isPublic,
            AnalyticsParams.HAS_SIZE_LIMIT to hasSizeLimit,
        ),
    )
}
