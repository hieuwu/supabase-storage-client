package com.hieuwu.supabasestorageclient.observability.analytics

/**
 * Which features earn the code they cost — search, stars, sharing, previews, settings.
 *
 * Lower stakes than activation or transfers, and read differently: these mostly answer "should we
 * keep investing here", so they favour counts and coarse dimensions over per-event detail.
 */
object EngagementEvents {
    const val SEARCH_PERFORMED = "search_performed"
    const val SEARCH_RESULT_OPENED = "search_result_opened"
    const val ITEM_STARRED = "item_starred"
    const val ITEM_UNSTARRED = "item_unstarred"
    const val STARRED_CLEARED = "starred_cleared"
    const val FILE_LINK_COPIED = "file_link_copied"
    const val FILE_PREVIEW_OPENED = "file_preview_opened"
    const val CONTENT_REFRESHED = "content_refreshed"
    const val SETTING_CHANGED = "setting_changed"
}

/** What was searched. Values for [AnalyticsParams.SCOPE]. */
object SearchScopes {
    const val BUCKETS = "buckets"
    const val FILES = "files"
}

/** Values for [AnalyticsParams.LINK_TYPE]. */
object LinkTypes {
    const val PUBLIC_URL = "public_url"
    const val PATH = "path"
}

/** Values for [AnalyticsParams.PREVIEW_TYPE] — which viewers are worth maintaining. */
object PreviewTypes {
    const val IMAGE = "image"
    const val GIF = "gif"
    const val VIDEO = "video"
    const val PDF = "pdf"
    const val UNSUPPORTED = "unsupported"
}

/** Values for [AnalyticsParams.SETTING]. */
object SettingKeys {
    const val THEME = "theme"
    const val VIEW_MODE = "view_mode"
    const val DOWNLOAD_PATH_MODE = "download_path_mode"
    const val DEFAULT_DOWNLOAD_DIRECTORY = "default_download_directory"
    const val FILE_SIZE_LIMIT = "file_size_limit"
    const val FILE_SIZE_UNIT = "file_size_unit"
}

/** [scope] is one of [SearchScopes]. [resultCount] answers "does search find anything?". */
fun AppAnalytics.logSearchPerformed(scope: String, resultCount: Int) {
    logEvent(
        EngagementEvents.SEARCH_PERFORMED,
        mapOf(
            AnalyticsParams.SCOPE to scope,
            AnalyticsParams.RESULT_COUNT to resultCount,
        ),
    )
}

/** Opening a result is the only proof the search was useful. */
fun AppAnalytics.logSearchResultOpened(scope: String, itemType: String) {
    logEvent(
        EngagementEvents.SEARCH_RESULT_OPENED,
        mapOf(
            AnalyticsParams.SCOPE to scope,
            AnalyticsParams.ITEM_TYPE to itemType,
        ),
    )
}

/** [itemType] is one of [ItemTypes], [surface] one of [AnalyticsSurfaces]. */
fun AppAnalytics.logItemStarred(itemType: String, surface: String) {
    logEvent(
        EngagementEvents.ITEM_STARRED,
        mapOf(
            AnalyticsParams.ITEM_TYPE to itemType,
            AnalyticsParams.SURFACE to surface,
        ),
    )
}

fun AppAnalytics.logItemUnstarred(itemType: String, surface: String) {
    logEvent(
        EngagementEvents.ITEM_UNSTARRED,
        mapOf(
            AnalyticsParams.ITEM_TYPE to itemType,
            AnalyticsParams.SURFACE to surface,
        ),
    )
}

fun AppAnalytics.logStarredCleared(itemCount: Int) {
    logEvent(EngagementEvents.STARRED_CLEARED, mapOf(AnalyticsParams.ITEM_COUNT to itemCount))
}

/** [linkType] is one of [LinkTypes]. Copying a public URL is the main sharing path. */
fun AppAnalytics.logFileLinkCopied(linkType: String, surface: String) {
    logEvent(
        EngagementEvents.FILE_LINK_COPIED,
        mapOf(
            AnalyticsParams.LINK_TYPE to linkType,
            AnalyticsParams.SURFACE to surface,
        ),
    )
}

/** [previewType] is one of [PreviewTypes]. */
fun AppAnalytics.logFilePreviewOpened(fileName: String, previewType: String) {
    logEvent(
        EngagementEvents.FILE_PREVIEW_OPENED,
        mapOf(
            AnalyticsParams.FILE_EXTENSION to fileExtensionOf(fileName),
            AnalyticsParams.PREVIEW_TYPE to previewType,
        ),
    )
}

/** Manual refresh is a complaint about stale data; [surface] is one of [AnalyticsSurfaces]. */
fun AppAnalytics.logContentRefreshed(surface: String) {
    logEvent(EngagementEvents.CONTENT_REFRESHED, mapOf(AnalyticsParams.SURFACE to surface))
}

/** [setting] is one of [SettingKeys]; [value] must be an enum-like token, never free text. */
fun AppAnalytics.logSettingChanged(setting: String, value: String) {
    logEvent(
        EngagementEvents.SETTING_CHANGED,
        mapOf(
            AnalyticsParams.SETTING to setting,
            AnalyticsParams.VALUE to value,
        ),
    )
}
