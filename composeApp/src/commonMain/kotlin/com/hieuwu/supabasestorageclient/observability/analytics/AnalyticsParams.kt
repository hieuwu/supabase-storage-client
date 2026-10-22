package com.hieuwu.supabasestorageclient.observability.analytics

/**
 * Parameter names shared across the tracking plan.
 *
 * These live together rather than with their events because the value of `reason`, `source` or
 * `surface` is that they mean the same thing everywhere — an exploration that groups by `reason`
 * across uploads, downloads and connection failures only works if there is one spelling of it.
 *
 * Firebase caps parameter names at 40 chars and string values at 100.
 */
object AnalyticsParams {
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"

    const val ACTION = "action"
    const val SUCCESS = "success"
    const val REASON = "reason"
    const val SOURCE = "source"
    const val SURFACE = "surface"

    const val SLIDE_INDEX = "slide_index"
    const val CONNECTION_COUNT = "connection_count"
    const val IS_FIRST = "is_first"

    const val FILE_EXTENSION = "file_extension"
    const val SIZE_BYTES = "size_bytes"
    const val SIZE_BUCKET = "size_bucket"
    const val DURATION_MS = "duration_ms"
    const val DESTINATION_MODE = "destination_mode"

    const val ITEM_TYPE = "item_type"
    const val ITEM_COUNT = "item_count"
    const val RESULT_COUNT = "result_count"
    const val SCOPE = "scope"
    const val LINK_TYPE = "link_type"
    const val PREVIEW_TYPE = "preview_type"

    const val SETTING = "setting"
    const val VALUE = "value"

    const val HAS_OFFERING = "has_offering"
    const val IS_PUBLIC = "is_public"
    const val HAS_SIZE_LIMIT = "has_size_limit"
    const val IS_PRO = "is_pro"
}

/** Where a flow was entered from. Values for [AnalyticsParams.SOURCE]. */
object AnalyticsSources {
    const val ONBOARDING = "onboarding"
    const val CREDENTIALS = "credentials"
    const val CONNECTION_LIMIT = "connection_limit"
    const val UPGRADE_BUTTON = "upgrade_button"
    const val SETTINGS = "settings"
    const val MAIN = "main"
    const val PAYWALL = "paywall"
}

/**
 * Which part of the app an action was taken from. Values for [AnalyticsParams.SURFACE].
 *
 * Separate from [AnalyticsScreens] on purpose: a surface says where an action happened, a screen
 * name says what was displayed, and conflating them makes both harder to read.
 */
object AnalyticsSurfaces {
    const val BUCKETS = "buckets"
    const val BUCKET_CONTENTS = "bucket_contents"
    const val FILE_VIEW = "file_view"
    const val STARRED = "starred"
    const val SEARCH = "search"
}

/** What was acted on. Values for [AnalyticsParams.ITEM_TYPE]. */
object ItemTypes {
    const val BUCKET = "bucket"
    const val FOLDER = "folder"
    const val FILE = "file"
}
