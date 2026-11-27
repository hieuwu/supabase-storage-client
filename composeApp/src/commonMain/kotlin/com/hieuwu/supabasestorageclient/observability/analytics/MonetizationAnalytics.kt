package com.hieuwu.supabasestorageclient.observability.analytics

/**
 * The paywall funnel. The app has exactly one paid gate — the third connection — so this is the
 * whole revenue picture on the client side.
 *
 * [PAYWALL_TRIGGERED] and [PAYWALL_SHOWN] are separate because the offering fetch can fail and the
 * paywall still renders, in a state nobody can buy from. A single "paywall viewed" event would
 * hide that as a conversion problem instead of the outage it is.
 *
 * Revenue itself comes from the store and RevenueCat; [PURCHASE_COMPLETED] exists to close this
 * funnel, not to report money.
 */
object MonetizationEvents {
    const val PAYWALL_TRIGGERED = "paywall_triggered"
    const val PAYWALL_SHOWN = "paywall_shown"
    const val PAYWALL_DISMISSED = "paywall_dismissed"
    const val PURCHASE_STARTED = "purchase_started"
    const val PURCHASE_COMPLETED = "purchase_completed"
    const val PURCHASE_CANCELLED = "purchase_cancelled"
    const val PURCHASE_FAILED = "purchase_failed"
    const val RESTORE_STARTED = "restore_started"
    const val RESTORE_COMPLETED = "restore_completed"
    const val RESTORE_FAILED = "restore_failed"
}

/** [source] is one of [AnalyticsSources] — which gate sent the user here. */
fun AppAnalytics.logPaywallTriggered(source: String) {
    logEvent(MonetizationEvents.PAYWALL_TRIGGERED, mapOf(AnalyticsParams.SOURCE to source))
}

fun AppAnalytics.logPaywallShown(hasOffering: Boolean) {
    logEvent(MonetizationEvents.PAYWALL_SHOWN, mapOf(AnalyticsParams.HAS_OFFERING to hasOffering))
}

fun AppAnalytics.logPaywallDismissed() {
    logEvent(MonetizationEvents.PAYWALL_DISMISSED)
}

fun AppAnalytics.logPurchaseStarted() {
    logEvent(MonetizationEvents.PURCHASE_STARTED)
}

fun AppAnalytics.logPurchaseCompleted() {
    logEvent(MonetizationEvents.PURCHASE_COMPLETED)
}

fun AppAnalytics.logPurchaseCancelled() {
    logEvent(MonetizationEvents.PURCHASE_CANCELLED)
}

/** [reason] comes from [purchaseErrorReason], never from the store's own message. */
fun AppAnalytics.logPurchaseFailed(reason: String) {
    logEvent(MonetizationEvents.PURCHASE_FAILED, mapOf(AnalyticsParams.REASON to reason))
}

fun AppAnalytics.logRestoreStarted(source: String) {
    logEvent(MonetizationEvents.RESTORE_STARTED, mapOf(AnalyticsParams.SOURCE to source))
}

/** [isPro] separates "restored something" from "restored nothing", which read very differently. */
fun AppAnalytics.logRestoreCompleted(source: String, isPro: Boolean) {
    logEvent(
        MonetizationEvents.RESTORE_COMPLETED,
        mapOf(
            AnalyticsParams.SOURCE to source,
            AnalyticsParams.IS_PRO to isPro,
        ),
    )
}

fun AppAnalytics.logRestoreFailed(source: String, reason: String) {
    logEvent(
        MonetizationEvents.RESTORE_FAILED,
        mapOf(
            AnalyticsParams.SOURCE to source,
            AnalyticsParams.REASON to reason,
        ),
    )
}

/**
 * Classifies a store/RevenueCat failure into a stable token.
 *
 * Matching on the message rather than the SDK's error code keeps this independent of the
 * RevenueCat API surface, and — more importantly — guarantees the raw message, which can name the
 * product and the store account, never reaches an event parameter.
 */
internal fun purchaseErrorReason(message: String?): String {
    val details = message.orEmpty().lowercase()
    return when {
        details.contains("cancel") -> "cancelled"
        details.contains("network") || details.contains("connection") || details.contains("offline") -> "network"
        details.contains("already own") || details.contains("already purchased") -> "already_owned"
        details.contains("not allowed") || details.contains("not permitted") -> "purchases_not_allowed"
        details.contains("unavailable") || details.contains("product") -> "product_unavailable"
        details.contains("payment") || details.contains("billing") -> "payment_declined"
        details.contains("configur") -> "misconfigured"
        details.contains("receipt") -> "receipt_invalid"
        details.isEmpty() -> "unknown"
        else -> "other"
    }
}
