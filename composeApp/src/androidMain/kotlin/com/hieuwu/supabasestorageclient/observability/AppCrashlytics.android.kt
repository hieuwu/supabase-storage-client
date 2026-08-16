package com.hieuwu.supabasestorageclient.observability

import com.google.firebase.crashlytics.FirebaseCrashlytics

actual object AppCrashlytics {

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    /**
     * No-op: `FirebaseApp` is initialised by `FirebaseInitProvider` from `google-services.json`
     * before `Application.onCreate()` runs.
     */
    actual fun initialize() = Unit

    actual fun log(message: String) {
        crashlytics.log(message)
    }

    actual fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    actual fun setUserId(userId: String?) {
        crashlytics.setUserId(userId ?: "")
    }

    actual fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Long) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Float) {
        crashlytics.setCustomKey(key, value)
    }

    actual fun setCustomKey(key: String, value: Double) {
        crashlytics.setCustomKey(key, value)
    }
}
