package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class LogFileParserTest {

    private fun entry(
        id: Long,
        level: LogLevel,
        tag: String,
        message: String,
        pid: Int,
    ) = LogEntry(
        id = id,
        epochSeconds = 1_791_540_000L + id,
        nanos = 352_000_000,
        pid = pid,
        tid = pid + 28,
        level = level,
        tag = tag,
        message = message,
    )

    @Test
    fun `reads back what export wrote, multi-line messages and tags with spaces too`() {
        val entries = listOf(
            entry(id = 1, level = LogLevel.ERROR, tag = "AndroidRuntime", message = "FATAL EXCEPTION: main\njava.lang.IllegalStateException: x\n\tat a.B.c(B.kt:1)", pid = 14_302),
            entry(id = 2, level = LogLevel.INFO, tag = "Ktor Client", message = "RESPONSE: 200 OK", pid = 14_302),
            entry(id = 3, level = LogLevel.DEBUG, tag = "art", message = "GC freed", pid = 99),
        )
        val text = LogCopyFormatter.format(entries = entries, mode = CopyMode.FULL, processNames = mapOf(14_302 to "com.app"))

        val parsed = LogFileParser.parse(text = text, zone = ZoneId.systemDefault())

        assertEquals(entries.map { it.message }, parsed.entries.map { it.message })
        assertEquals(entries.map { it.tag }, parsed.entries.map { it.tag })
        assertEquals(entries.map { it.level }, parsed.entries.map { it.level })
        assertEquals(entries.map { it.epochSeconds }, parsed.entries.map { it.epochSeconds })
        assertEquals(entries.map { it.tid }, parsed.entries.map { it.tid })
        assertEquals(mapOf(14_302 to "com.app"), parsed.processNames)
    }

    @Test
    fun `lines before the first entry are skipped`() {
        assertEquals(emptyList<LogEntry>(), LogFileParser.parse(text = "--------- beginning of main\nnot a log").entries)
    }
}
