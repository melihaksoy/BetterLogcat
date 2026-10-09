package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable

/** Paints every match of [pattern] in messages with [colorArgb]. */
@Immutable
data class HighlightRule(
    val pattern: String,
    val colorArgb: Long,
    val isRegex: Boolean,
) {
    /** One line for the settings file: `AARRGGBB<tab>regex flag<tab>pattern`. */
    fun encode(): String = "${colorArgb.toString(radix = 16)}\t${if (isRegex) 1 else 0}\t$pattern"

    companion object {
        fun decode(line: String): HighlightRule? {
            val parts = line.split('\t', limit = 3)
            if (parts.size != 3 || parts[2].isEmpty()) return null
            return HighlightRule(
                pattern = parts[2],
                colorArgb = parts[0].toLongOrNull(radix = 16) ?: return null,
                isRegex = parts[1] == "1",
            )
        }
    }
}
