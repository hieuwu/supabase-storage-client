package com.hieuwu.supabasestorageclient.observability.analytics

/**
 * Firebase Analytics for this app: the entry point, and the rules the rest of the package follows.
 *
 * The tracking plan answers four questions, in order of how much they are worth. Each has its own
 * file, holding its event names, its enumerated parameter values, and the typed helpers that are
 * the only supported way to emit them:
 *
 * | File | Question |
 * |---|---|
 * | [ActivationEvents] in `ActivationAnalytics.kt` | Do people get past "paste your Supabase URL and service key"? That step is the only thing between install and any value at all, and it fails for reasons the user cannot diagnose. |
 * | [TransferEvents] in `TransferAnalytics.kt` | Do uploads and downloads actually succeed? The app exists to move files. |
 * | [MonetizationEvents] in `MonetizationAnalytics.kt` | The single paid gate is the third connection. Is it placed where people feel it? |
 * | [StorageEvents] in `StorageAnalytics.kt`, [EngagementEvents] in `EngagementAnalytics.kt` | Which features earn the code they cost? |
 *
 * Supporting files: `AnalyticsParams.kt` (shared parameter names and the vocabulary used across
 * domains), `AnalyticsValues.kt` (the lossy shaping every value passes through),
 * `AnalyticsUserProperties.kt` (the slicing dimensions), `ScreenAnalytics.kt` (screen views).
 *
 * Rules for anything added to this package:
 *
 * * Firebase names are `[a-zA-Z][a-zA-Z0-9_]*`, ≤ 40 chars, no `firebase_` / `google_` / `ga_`
 *   prefix, 500 distinct event names per app. `snake_case`, past tense.
 * * Nothing user-identifying ever becomes a parameter — no bucket names, object paths, file names,
 *   project refs, URLs, or key material. File names are accepted by the helpers but only the
 *   extension survives ([fileExtensionOf]). Analytics data is retained server-side and feeds
 *   audience building, so this is a firmer line than the one Crashlytics draws.
 * * Free-form error text never becomes a parameter either: it is unbounded cardinality and often
 *   contains a path. Errors are classified first — [storageErrorReason], [purchaseErrorReason].
 * * No `setUserId`. The app has no accounts; the only identity available is the user's Supabase
 *   project, and sending that would tie an install to someone's infrastructure for no product
 *   question we actually have. Firebase's pseudonymous instance id covers retention and funnels.
 * * Nothing duplicates what Firebase collects on its own — `first_open`, `session_start`,
 *   `app_update`, and store purchase events arrive automatically.
 * * Events and user properties are emitted from the code that owns the thing being reported, not
 *   from a central observer. A collector added just to watch state for analytics is a duplicate
 *   subscription, and on a cold flow it is a duplicate query for the lifetime of the app.
 */
fun initAnalytics() {
    AppAnalytics.initialize()
}
