package com.hieuwu.supabasestorageclient.core

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant as KotlinInstant
import kotlinx.datetime.Instant as KxInstant

/**
 * Formats an Instant into a more human-readable format.
 * Example: 2023-10-24T10:00:00Z -> Oct 24, 2023
 */
fun formatDate(instant: Instant?): String {
    if (instant == null) return "Unknown"
    return try {
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = when (dateTime.monthNumber) {
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
        "$month ${dateTime.dayOfMonth}, ${dateTime.year}"
    } catch (e: Exception) {
        instant.toString().take(10) // Fallback to YYYY-MM-DD
    }
}

fun formatDateTime(instant: Instant?): String {
    if (instant == null) return "Unknown"
    return try {
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = when (dateTime.monthNumber) {
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
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        "$month ${dateTime.dayOfMonth}, ${dateTime.year} $hour:$minute"
    } catch (e: Exception) {
        instant.toString().take(16)
    }
}

/**
 * Formats an ISO 8601 date string into a more human-readable format.
 * Example: 2023-10-24T10:00:00Z -> Oct 24, 2023
 */
fun formatDate(isoString: String?): String {
    if (isoString == null) return "Unknown"
    return try {
        val instant = Instant.parse(isoString)
        formatDate(instant)
    } catch (e: Exception) {
        isoString.take(10) // Fallback to YYYY-MM-DD
    }
}

fun KotlinInstant.toKxInstant(): KxInstant =
    KxInstant.fromEpochMilliseconds(this.toEpochMilliseconds())

/** Convert kotlinx.datetime.Instant → kotlin.time.Instant */
fun KxInstant.toKotlinInstant(): KotlinInstant =
    KotlinInstant.fromEpochMilliseconds(this.toEpochMilliseconds())