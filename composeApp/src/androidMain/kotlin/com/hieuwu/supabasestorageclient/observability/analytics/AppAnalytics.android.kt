package com.hieuwu.supabasestorageclient.observability.analytics

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

actual object AppAnalytics {

    private val analytics: FirebaseAnalytics
        get() = Firebase.analytics

    /**
     * No-op: `FirebaseApp` is initialised by `FirebaseInitProvider` from `google-services.json`
     * before `Application.onCreate()` runs.
     */
    actual fun initialize() = Unit

    actual fun setEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    actual fun logEvent(name: String, params: Map<String, Any?>) {
        analytics.logEvent(name, params.toBundle())
    }

    actual fun setUserId(userId: String?) {
        analytics.setUserId(userId)
    }

    actual fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    private fun Map<String, Any?>.toBundle(): Bundle? {
        val normalized = normalizeAnalyticsParams(this)
        if (normalized.isEmpty()) return null
        return Bundle(normalized.size).apply {
            normalized.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                }
            }
        }
    }
}
