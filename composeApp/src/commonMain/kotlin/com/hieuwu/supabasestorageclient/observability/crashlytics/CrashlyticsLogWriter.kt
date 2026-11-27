package com.hieuwu.supabasestorageclient.observability.crashlytics

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/**
 * Kermit [LogWriter] that mirrors logs into Crashlytics: every record becomes a breadcrumb,
 * and anything at [Severity.Error] or above with a [Throwable] is also reported as a non-fatal.
 *
 * Install it alongside the platform writer, e.g.
 * `Logger.setLogWriters(platformLogWriter(), CrashlyticsLogWriter())`.
 */
class CrashlyticsLogWriter(
    private val minSeverity: Severity = Severity.Info,
    private val nonFatalSeverity: Severity = Severity.Error,
) : LogWriter() {

    override fun isLoggable(tag: String, severity: Severity): Boolean = severity >= minSeverity

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        AppCrashlytics.log("[${severity.name}] $tag: $message")
        if (throwable != null && severity >= nonFatalSeverity) {
            AppCrashlytics.recordException(throwable)
        }
    }
}
