package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogCopyFormatterTest {

    private val trace = entry(
        id = 1,
        message = "boom\n\tat com.example.Foo.bar(Foo.kt:12)",
    )
    private val json = entry(
        id = 2,
        message = """Response: {"id":7,"ok":true}""",
    )

    @Test
    fun `full copy aligns continuation lines under the message`() {
        val text = LogCopyFormatter.format(
            entries = listOf(trace),
            mode = CopyMode.FULL,
            processNames = mapOf(100 to "com.example.app"),
        )

        val lines = text.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].endsWith("com.example.app E Garage: boom"))
        assertEquals(lines[0].indexOf("boom"), lines[1].indexOf("\tat"))
    }

    @Test
    fun `message copy drops headers`() {
        val text = LogCopyFormatter.format(
            entries = listOf(trace, json),
            mode = CopyMode.MESSAGE,
            processNames = emptyMap(),
        )

        assertEquals("boom\n\tat com.example.Foo.bar(Foo.kt:12)\nResponse: {\"id\":7,\"ok\":true}", text)
    }

    @Test
    fun `json copy prints only the json part`() {
        val pretty = LogCopyFormatter.format(
            entries = listOf(json),
            mode = CopyMode.JSON_PRETTY,
            processNames = emptyMap(),
        )
        val minified = LogCopyFormatter.format(
            entries = listOf(json),
            mode = CopyMode.JSON_MINIFIED,
            processNames = emptyMap(),
        )

        assertEquals("{\n  \"id\": 7,\n  \"ok\": true\n}", pretty)
        assertEquals("""{"id":7,"ok":true}""", minified)
    }

    private fun entry(
        id: Long,
        message: String,
    ) = LogEntry(
        id = id,
        epochSeconds = 1_791_540_000L,
        nanos = 0,
        pid = 100,
        tid = 101,
        level = LogLevel.ERROR,
        tag = "Garage",
        message = message,
    )
}
