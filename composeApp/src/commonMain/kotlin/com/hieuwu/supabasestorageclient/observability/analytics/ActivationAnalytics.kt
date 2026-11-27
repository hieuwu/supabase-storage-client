package com.hieuwu.supabasestorageclient.observability.analytics

import com.hieuwu.supabasestorageclient.domain.error.storageErrorReason

/**
 * Activation: install → onboarding → a Supabase connection that actually works.
 *
 * The highest-value events in the plan. A connection is one saved set of credentials, and the
 * moment that matters is verification, not saving — [CONNECTION_ADDED] is free and means nothing,
 * [CONNECTION_ACTIVATED] means the user's key worked against their live project.
 * [CONNECTION_ACTIVATION_FAILED] carries the classified reason, which is the difference between
 * "our docs are wrong" (`invalid_key`), "we should explain RLS" (`rls_policy`) and "nothing is
 * wrong with the product" (`network`).
 */
object ActivationEvents {
    /** One per onboarding page reached. Locates the drop-off, including the security warning. */
    const val ONBOARDING_SLIDE_VIEWED = "onboarding_slide_viewed"
    const val ONBOARDING_COMPLETED = "onboarding_completed"

    const val CONNECTION_ADD_OPENED = "connection_add_opened"
    const val CONNECTION_ADDED = "connection_added"
    const val CONNECTION_ADD_FAILED = "connection_add_failed"

    /** Verified against the live project — the real activation moment. */
    const val CONNECTION_ACTIVATED = "connection_activated"
    const val CONNECTION_ACTIVATION_FAILED = "connection_activation_failed"
    const val CONNECTION_REMOVED = "connection_removed"
}

fun AppAnalytics.logOnboardingSlideViewed(slideIndex: Int) {
    logEvent(
        ActivationEvents.ONBOARDING_SLIDE_VIEWED,
        mapOf(AnalyticsParams.SLIDE_INDEX to slideIndex),
    )
}

fun AppAnalytics.logOnboardingCompleted() {
    logEvent(ActivationEvents.ONBOARDING_COMPLETED)
}

/** [source] is one of [AnalyticsSources]. */
fun AppAnalytics.logConnectionAddOpened(source: String) {
    logEvent(ActivationEvents.CONNECTION_ADD_OPENED, mapOf(AnalyticsParams.SOURCE to source))
}

fun AppAnalytics.logConnectionAdded(connectionCount: Int) {
    logEvent(
        ActivationEvents.CONNECTION_ADDED,
        mapOf(AnalyticsParams.CONNECTION_COUNT to connectionCount),
    )
}

fun AppAnalytics.logConnectionAddFailed(error: Throwable) {
    logEvent(
        ActivationEvents.CONNECTION_ADD_FAILED,
        mapOf(AnalyticsParams.REASON to storageErrorReason(error).key),
    )
}

/**
 * The connection was verified against the live project. [isFirst] separates activation from every
 * later switch, which is the difference between a funnel and a usage count.
 */
fun AppAnalytics.logConnectionActivated(connectionCount: Int, isFirst: Boolean) {
    logEvent(
        ActivationEvents.CONNECTION_ACTIVATED,
        mapOf(
            AnalyticsParams.CONNECTION_COUNT to connectionCount,
            AnalyticsParams.IS_FIRST to isFirst,
        ),
    )
}

fun AppAnalytics.logConnectionActivationFailed(error: Throwable) {
    logEvent(
        ActivationEvents.CONNECTION_ACTIVATION_FAILED,
        mapOf(AnalyticsParams.REASON to storageErrorReason(error).key),
    )
}

fun AppAnalytics.logConnectionRemoved(connectionCount: Int) {
    logEvent(
        ActivationEvents.CONNECTION_REMOVED,
        mapOf(AnalyticsParams.CONNECTION_COUNT to connectionCount),
    )
}
