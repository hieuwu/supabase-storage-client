package com.hieuwu.supabasestorageclient.observability

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter

/**
 * Initialises Crashlytics and routes Kermit output into it.
 * Call once from each platform entry point, before Koin/UI start up.
 */
fun initCrashReporting() {
    AppCrashlytics.initialize()
    Logger.setLogWriters(platformLogWriter(), CrashlyticsLogWriter())
}
