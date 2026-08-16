package com.hieuwu.supabasestorageclient.observability

/**
 * Multiplatform facade over Firebase Crashlytics.
 *
 * Android and iOS forward to the official Firebase SDKs; JVM and web are no-ops that
 * mirror the calls into Kermit so shared code can call this unconditionally.
 */
expect object AppCrashlytics {

    /**
     * Must be called once, as early as possible in the app lifecycle.
     *
     * Android is already initialised by the Firebase content provider, so this is a no-op there.
     * iOS calls `FIRApp.configure()` and requires `GoogleService-Info.plist` in the app bundle.
     */
    fun initialize()

    /** Adds a breadcrumb to the next crash/non-fatal report. */
    fun log(message: String)

    /** Reports [throwable] as a non-fatal event. */
    fun recordException(throwable: Throwable)

    /** Associates subsequent reports with [userId]; pass `null` to clear it. */
    fun setUserId(userId: String?)

    fun setCustomKey(key: String, value: String)

    fun setCustomKey(key: String, value: Boolean)

    fun setCustomKey(key: String, value: Int)

    fun setCustomKey(key: String, value: Long)

    fun setCustomKey(key: String, value: Float)

    fun setCustomKey(key: String, value: Double)
}
