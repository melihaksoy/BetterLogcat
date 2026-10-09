package com.melihaksoy.betterlogcat.ui

import androidx.compose.ui.text.AnnotatedString
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.ui.logcat.component.ItemRef
import com.melihaksoy.betterlogcat.ui.logcat.component.LogListLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class LogListLayoutTest {

    private val entries = (0 until 6).map { index ->
        LogEntry(
            id = index + 1L,
            epochSeconds = 1_791_540_000L,
            nanos = 0,
            pid = 1,
            tid = 1,
            level = LogLevel.INFO,
            tag = "Garage",
            message = "entry $index",
        )
    }

    // Entry 1 has 3 pieces (2 extra items), entry 4 has 2 pieces (1 extra item).
    private val layout = LogListLayout(
        entries = entries,
        chunked = mapOf(
            1 to List(size = 3) { AnnotatedString(text = "piece $it") },
            4 to List(size = 2) { AnnotatedString(text = "piece $it") },
        ),
    )

    @Test
    fun `items map to entries and pieces`() {
        assertEquals(9, layout.itemCount)
        assertEquals(
            listOf(
                ItemRef(entryIndex = 0, chunk = 0),
                ItemRef(entryIndex = 1, chunk = 0),
                ItemRef(entryIndex = 1, chunk = 1),
                ItemRef(entryIndex = 1, chunk = 2),
                ItemRef(entryIndex = 2, chunk = 0),
                ItemRef(entryIndex = 3, chunk = 0),
                ItemRef(entryIndex = 4, chunk = 0),
                ItemRef(entryIndex = 4, chunk = 1),
                ItemRef(entryIndex = 5, chunk = 0),
            ),
            (0 until layout.itemCount).map { layout.itemAt(item = it) },
        )
    }

    @Test
    fun `first item of each entry`() {
        assertEquals(listOf(0, 1, 4, 5, 6, 8), entries.indices.map { layout.firstItemOf(entryIndex = it) })
    }

    @Test
    fun `split keeps lines together in pieces`() {
        val text = AnnotatedString(text = (1..45).joinToString(separator = "\n") { "line $it" })

        val pieces = LogListLayout.split(
            text = text,
            linesPerPiece = 20,
        )

        assertEquals(3, pieces.size)
        assertEquals("line 1", pieces.first().text.lines().first())
        assertEquals("line 21", pieces[1].text.lines().first())
        assertEquals("line 45", pieces.last().text.lines().last())
    }

    @Test
    fun `a key still being typed does not filter`() {
        assertEquals("", LogFilter(query = "package:mine pack").effective().query)
        assertEquals("", LogFilter(query = "package:mine tag:").effective().query)
        assertEquals("timeout", LogFilter(query = "package:mine timeout").effective().query)
        assertEquals("pay", LogFilter(query = "pay").effective().query)
    }
}
