package com.hieuwu.supabasestorageclient.observability.analytics

/**
 * The slicing dimensions — what turns every event into a comparison: free vs pro, one connection
 * vs many, list vs grid.
 *
 * Firebase allows 25 per project (name ≤ 24 chars, value ≤ 36 chars).
 *
 * Each of these is published by the repository that owns the state, at the moment it changes:
 * `CredentialRepositoryImpl` for [CONNECTION_COUNT], `SettingsRepositoryImpl` for [THEME] /
 * [VIEW_MODE] / [DOWNLOAD_PATH_MODE], `PurchaseRepositoryImpl` for [IS_PRO]. Adding one here means
 * finding its owner, not adding a collector — see the rules in `Analytics.kt`.
 */
object AnalyticsUserProperties {
    const val IS_PRO = "is_pro"
    const val CONNECTION_COUNT = "connection_count"
    const val THEME = "theme"
    const val VIEW_MODE = "view_mode"
    const val DOWNLOAD_PATH_MODE = "download_path_mode"
}
