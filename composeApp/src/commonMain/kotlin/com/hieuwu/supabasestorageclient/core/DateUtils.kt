package com.hieuwu.supabasestorageclient.core

import co.touchlab.kermit.Logger
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant as KotlinInstant
import kotlinx.datetime.Instant as KxInstant

private val dateLogger = Logger.withTag("DateUtils")

private fun monthName(monthNumber: Int): String = when (monthNumber) {
    1 -> "Jan"
    2 -> "Feb"
    3 -> "Mar"
    4 -> "Apr"
    5 -> "May"
    6 -> "Jun"
    7 -> "Jul"
    8 -> "Aug"
    9 -> "Sep"
    10 -> "Oct"
    11 -> "Nov"
    12 -> "Dec"
    else -> ""
}

/**
 * Formats an Instant into a more human-readable format.
 * Example: 2023-10-24T10:00:00Z -> Oct 24, 2023
 */
fun formatDate(instant: Instant?): String {
    if (instant == null) return "Unknown"
    return runCatching {
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${monthName(dateTime.monthNumber)} ${dateTime.dayOfMonth}, ${dateTime.year}"
    }.getOrElse { error ->
        dateLogger.w(error) { "Failed to format date for $instant, falling back to ISO prefix" }
        instant.toString().take(10) // Fallback to YYYY-MM-DD
    }
}

fun formatDateTime(instant: Instant?): String {
    if (instant == null) return "Unknown"
    return runCatching {
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        "${monthName(dateTime.monthNumber)} ${dateTime.dayOfMonth}, ${dateTime.year} $hour:$minute"
    }.getOrElse { error ->
        dateLogger.w(error) { "Failed to format date-time for $instant, falling back to ISO prefix" }
        instant.toString().take(16)
    }
}

/**
 * Formats an ISO 8601 date string into a more human-readable format.
 * Example: 2023-10-24T10:00:00Z -> Oct 24, 2023
 */
fun formatDate(isoString: String?): String {
    if (isoString == null) return "Unknown"
    return runCatching { formatDate(Instant.parse(isoString)) }
        .getOrElse { error ->
            dateLogger.w(error) { "Failed to parse ISO date '$isoString', falling back to raw prefix" }
            isoString.take(10) // Fallback to YYYY-MM-DD
        }
}

/**
 * Parses an ISO 8601 string into an Instant, returning null (and logging) when the value is
 * malformed - used for cached values that may have been written by an older app version.
 */
fun parseInstantOrNull(isoString: String?, context: String): Instant? {
    if (isoString == null) return null
    return runCatching { Instant.parse(isoString) }
        .getOrElse { error ->
            dateLogger.w(error) { "Failed to parse instant '$isoString' for $context" }
            null
        }
}

fun KotlinInstant.toKxInstant(): KxInstant =
    KxInstant.fromEpochMilliseconds(this.toEpochMilliseconds())

/** Convert kotlinx.datetime.Instant → kotlin.time.Instant */
fun KxInstant.toKotlinInstant(): KotlinInstant =
    KotlinInstant.fromEpochMilliseconds(this.toEpochMilliseconds())