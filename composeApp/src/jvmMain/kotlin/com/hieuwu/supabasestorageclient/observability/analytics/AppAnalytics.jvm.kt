package com.hieuwu.supabasestorageclient.observability.analytics

import co.touchlab.kermit.Logger

/**
 * Desktop has no Firebase Analytics SDK; calls are mirrored into Kermit so shared code keeps
 * working without platform checks.
 */
actual object AppAnalytics {

    private val logger = Logger.withTag("AppAnalytics")

    actual fun initialize() = Unit

    actual fun setEnabled(enabled: Boolean) {
        logger.d { "setEnabled($enabled)" }
    }

    actual fun logEvent(name: String, params: Map<String, Any?>) {
        logger.d { "event $name ${normalizeAnalyticsParams(params)}" }
    }

    actual fun setUserId(userId: String?) {
        logger.d { "setUserId($userId)" }
    }

    actual fun setUserProperty(name: String, value: String?) {
        logger.d { "setUserProperty($name = $value)" }
    }
}
