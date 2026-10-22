package com.hieuwu.supabasestorageclient.observability.crashlytics

import co.touchlab.kermit.Logger

/**
 * Desktop has no Firebase Crashlytics SDK; calls are mirrored into Kermit so shared code keeps
 * working without platform checks.
 */
actual object AppCrashlytics {

    private val logger = Logger.withTag("AppCrashlytics")

    actual fun initialize() = Unit

    actual fun log(message: String) {
        logger.d { message }
    }

    actual fun recordException(throwable: Throwable) {
        logger.e(throwable) { "Non-fatal: ${throwable.message}" }
    }

    actual fun setUserId(userId: String?) {
        logger.d { "setUserId($userId)" }
    }

    actual fun setCustomKey(key: String, value: String) = logKey(key, value)

    actual fun setCustomKey(key: String, value: Boolean) = logKey(key, value)

    actual fun setCustomKey(key: String, value: Int) = logKey(key, value)

    actual fun setCustomKey(key: String, value: Long) = logKey(key, value)

    actual fun setCustomKey(key: String, value: Float) = logKey(key, value)

    actual fun setCustomKey(key: String, value: Double) = logKey(key, value)

    private fun logKey(key: String, value: Any) {
        logger.d { "setCustomKey($key = $value)" }
    }
}
