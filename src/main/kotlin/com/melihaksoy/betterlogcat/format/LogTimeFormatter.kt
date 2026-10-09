package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.TimeFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object LogTimeFormatter {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    private val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    /** [previous] is the row above, used only by [TimeFormat.DELTA]. */
    fun format(
        entry: LogEntry,
        format: TimeFormat,
        previous: LogEntry?,
    ): String = when (format) {
        TimeFormat.TIME -> timeFormatter.format(entry.instant())
        TimeFormat.DATE_TIME -> dateTimeFormatter.format(entry.instant())
        TimeFormat.FULL -> fullFormatter.format(entry.instant())
        TimeFormat.EPOCH -> "${entry.epochSeconds}.${(entry.nanos / NANOS_PER_MILLI).toString().padStart(length = 3, padChar = '0')}"
        TimeFormat.DELTA -> delta(
            entry = entry,
            previous = previous,
        )
    }

    fun dateTime(
        epochSeconds: Long,
        nanos: Int,
    ): String = fullFormatter.format(Instant.ofEpochSecond(epochSeconds, nanos.toLong()))

    private fun delta(
        entry: LogEntry,
        previous: LogEntry?,
    ): String {
        if (previous == null) return "+0.000s"
        val millis = (entry.epochMillis() - previous.epochMillis()).coerceAtLeast(minimumValue = 0L)
        return if (millis < MILLIS_PER_MINUTE) {
            String.format(Locale.ROOT, "+%d.%03ds", millis / 1_000, millis % 1_000)
        } else {
            String.format(Locale.ROOT, "+%dm%02ds", millis / MILLIS_PER_MINUTE, millis % MILLIS_PER_MINUTE / 1_000)
        }
    }

    private fun LogEntry.instant(): Instant = Instant.ofEpochSecond(epochSeconds, nanos.toLong())

    private fun LogEntry.epochMillis(): Long = epochSeconds * 1_000 + nanos / NANOS_PER_MILLI

    private const val NANOS_PER_MILLI = 1_000_000
    private const val MILLIS_PER_MINUTE = 60_000L
}
