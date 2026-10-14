package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkMergeTest {

    private val json = buildString {
        append("{\"items\":[")
        repeat(times = 120) { index ->
            if (index > 0) append(',')
            append("{\"id\":$index,\"name\":\"pokemon-$index\",\"url\":\"https://pokeapi.co/api/v2/pokemon/$index/\"}")
        }
        append("]}")
    }

    @Test
    fun `pieces of a long message join into one entry`() {
        val buffer = LogBuffer(capacity = 10_000)
        val pieces = json.chunked(size = 4_000)

        buffer.append(entries = pieces.mapIndexed { index, piece -> entry(id = index + 1L, message = piece, millis = index.toLong()) })

        val entries = buffer.visibleSnapshot()
        assertEquals(1, entries.size)
        assertEquals(json, entries.single().message)
        assertTrue(entries.single().json is JsonContent.Found)
        assertEquals(1, buffer.tagCountsSnapshot()["Ktor Client"])
    }

    @Test
    fun `a piece cut at a line break joins with a newline`() {
        val buffer = LogBuffer(capacity = 10_000)
        val first = "a".repeat(n = 3_500)

        buffer.append(
            entries = listOf(
                entry(id = 1, message = first, millis = 0),
                entry(id = 2, message = "next line", millis = 1),
            ),
        )

        assertEquals(first + "\nnext line", buffer.visibleSnapshot().single().message)
    }

    @Test
    fun `short messages and other threads do not join`() {
        val buffer = LogBuffer(capacity = 10_000)

        buffer.append(
            entries = listOf(
                entry(id = 1, message = "short", millis = 0),
                entry(id = 2, message = "also short", millis = 1),
                entry(id = 3, message = "x".repeat(n = 4_000), millis = 2),
                entry(id = 4, message = "other thread", millis = 3, tid = 99),
                entry(id = 5, message = "much later", millis = 5_000),
            ),
        )

        assertEquals(5, buffer.visibleSnapshot().size)
    }

    @Test
    fun `json is found between other text`() {
        val content = com.melihaksoy.betterlogcat.format.JsonFormatter.detect(
            message = "RESPONSE: 200 OK\nBODY START\n{\"id\":7,\"ok\":true}\nBODY END",
        )

        assertTrue(content is JsonContent.Found)
        content as JsonContent.Found
        assertEquals("RESPONSE: 200 OK\nBODY START\n", content.prefix)
        assertEquals("\nBODY END", content.suffix)
        assertEquals("""{"id":7,"ok":true}""", content.minified)
    }

    private fun entry(
        id: Long,
        message: String,
        millis: Long,
        tid: Int = 5210,
    ) = LogEntry(
        id = id,
        epochSeconds = 1_791_540_000L + millis / 1_000,
        nanos = (millis % 1_000 * 1_000_000).toInt(),
        pid = 2904,
        tid = tid,
        level = LogLevel.INFO,
        tag = "Ktor Client",
        message = message,
    )
}
