package com.hieuwu.supabasestorageclient.observability.analytics

/**
 * Multiplatform facade over Firebase Analytics.
 *
 * Android and iOS forward to the official Firebase SDKs; JVM and web are no-ops that
 * mirror the calls into Kermit so shared code can call this unconditionally.
 *
 * This is the transport, not the tracking plan. Call sites use the typed helpers in the
 * `*Analytics.kt` files rather than [logEvent] with raw strings; `Analytics.kt` maps out which
 * file owns what.
 */
expect object AppAnalytics {

    /**
     * Must be called once, as early as possible in the app lifecycle.
     *
     * Android is already initialised by the Firebase content provider, so this is a no-op there.
     * iOS makes sure `FIRApp.configure()` has run; it requires `GoogleService-Info.plist` in the
     * app bundle.
     */
    fun initialize()

    /**
     * Turns collection on/off at runtime (consent screens, debug builds, opt-out settings).
     * The value is persisted by the SDK across launches.
     */
    fun setEnabled(enabled: Boolean)

    /**
     * Logs [name] with [params].
     *
     * Firebase only accepts `String`, `Long` and `Double` parameter values; anything else is
     * normalised by [normalizeAnalyticsParams] before it reaches the SDK, and `null` values are
     * dropped.
     */
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())

    /** Associates subsequent events with [userId]; pass `null` to clear it. */
    fun setUserId(userId: String?)

    /** Sets a user property used for audience/segment filtering; pass `null` to clear it. */
    fun setUserProperty(name: String, value: String?)
}

/**
 * Firebase parameter values may only be `String`, `Long` or `Double`.
 * Booleans become `1`/`0`, other numbers are widened, everything else falls back to `toString()`.
 * Entries with a `null` value are dropped.
 */
internal fun normalizeAnalyticsParams(params: Map<String, Any?>): Map<String, Any> =
    buildMap(params.size) {
        params.forEach { (key, value) ->
            val normalized: Any = when (value) {
                null -> return@forEach
                is String -> value
                is Boolean -> if (value) 1L else 0L
                is Long -> value
                is Int -> value.toLong()
                is Short -> value.toLong()
                is Byte -> value.toLong()
                is Double -> value
                is Float -> value.toDouble()
                else -> value.toString()
            }
            put(key, normalized)
        }
    }
