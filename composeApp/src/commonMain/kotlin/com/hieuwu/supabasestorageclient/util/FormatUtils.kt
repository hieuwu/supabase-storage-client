package com.hieuwu.supabasestorageclient.util

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Formats a double value with a specific number of decimal places.
 * This is a cross-platform alternative to String.format which is JVM-only.
 */
fun Double.format(decimals: Int): String {
    val multiplier = decimals.toDouble().pow(10.0)
    val rounded = (this * multiplier).roundToLong() / multiplier
    val s = rounded.toString()
    
    return if (decimals > 0) {
        val parts = s.split(".")
        val intPart = parts[0]
        val fractionPart = if (parts.size > 1) parts[1] else ""
        val paddedFraction = fractionPart.padEnd(decimals, '0').substring(0, decimals)
        "$intPart.$paddedFraction"
    } else {
        s.split(".")[0]
    }
}

fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "${gb.toLong()} GB"
        mb >= 1 -> "${mb.toLong()} MB"
        kb >= 1 -> "${kb.toLong()} KB"
        else -> "$bytes Bytes"
    }
}
