package com.hieuwu.supabasestorageclient.observability.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** Firebase's reserved screen-view event. Compose Multiplatform does not emit it automatically. */
private const val SCREEN_VIEW = "screen_view"

/**
 * Screen names, kept together so they stay comparable.
 *
 * Each screen composable calls [TrackScreenView] with its own name. That is deliberately not done
 * from the navigation graph: `BucketScreen` and `FileViewScreen` are registered in two different
 * `NavHost`s, and a screen that names itself cannot disagree with itself.
 */
object AnalyticsScreens {
    const val ONBOARDING = "onboarding"
    const val CREDENTIALS = "credentials"
    const val BUCKETS = "buckets"
    const val BUCKET_CONTENTS = "bucket_contents"
    const val FILE_VIEW = "file_view"
    const val STARRED = "starred"
    const val DOWNLOADS = "downloads"
    const val UPLOADS = "uploads"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val ABOUT = "about"
    const val PAYWALL = "paywall"
}

/**
 * Logs a screen view. [screenClass] defaults to [screenName] so the Firebase console groups
 * screens consistently across platforms.
 */
fun AppAnalytics.logScreenView(screenName: String, screenClass: String = screenName) {
    logEvent(
        SCREEN_VIEW,
        mapOf(
            AnalyticsParams.SCREEN_NAME to screenName,
            AnalyticsParams.SCREEN_CLASS to screenClass,
        ),
    )
}

/**
 * Logs a `screen_view` for [screenName] when the composable enters composition, and again whenever
 * [screenName] changes.
 */
@Composable
fun TrackScreenView(screenName: String, screenClass: String = screenName) {
    LaunchedEffect(screenName, screenClass) {
        AppAnalytics.logScreenView(screenName, screenClass)
    }
}
