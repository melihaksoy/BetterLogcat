package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogTimeFormatterTest {

    private val first = entry(
        epochSeconds = 1_791_540_001L,
        nanos = 37_000_000,
    )
    private val second = entry(
        epochSeconds = 1_791_540_002L,
        nanos = 250_000_000,
    )

    @Test
    fun `epoch keeps milliseconds`() {
        assertEquals(
            "1791540001.037",
            LogTimeFormatter.format(
                entry = first,
                format = TimeFormat.EPOCH,
                previous = null,
            ),
        )
    }

    @Test
    fun `delta measures from the row above`() {
        assertEquals(
            "+1.213s",
            LogTimeFormatter.format(
                entry = second,
                format = TimeFormat.DELTA,
                previous = first,
            ),
        )
        assertEquals(
            "+0.000s",
            LogTimeFormatter.format(
                entry = first,
                format = TimeFormat.DELTA,
                previous = null,
            ),
        )
    }

    @Test
    fun `clock formats have the expected shape`() {
        val time = LogTimeFormatter.format(
            entry = first,
            format = TimeFormat.TIME,
            previous = null,
        )
        val full = LogTimeFormatter.format(
            entry = first,
            format = TimeFormat.FULL,
            previous = null,
        )

        assertTrue(time.matches(Regex(pattern = """\d{2}:\d{2}:\d{2}\.037""")))
        assertTrue(full.matches(Regex(pattern = """\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.037""")))
    }

    private fun entry(
        epochSeconds: Long,
        nanos: Int,
    ) = LogEntry(
        id = epochSeconds,
        epochSeconds = epochSeconds,
        nanos = nanos,
        pid = 1,
        tid = 1,
        level = LogLevel.INFO,
        tag = "Garage",
        message = "tick",
    )
}
