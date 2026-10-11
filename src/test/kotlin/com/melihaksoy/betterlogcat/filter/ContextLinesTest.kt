package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ContextLinesTest {

    private fun entries(vararg messages: String) = messages.mapIndexed { index, message ->
        LogEntry(
            id = index + 1L,
            epochSeconds = 1_791_540_000L + index,
            nanos = 0,
            pid = 1,
            tid = 1,
            level = LogLevel.INFO,
            tag = "Tag",
            message = message,
        )
    }

    private fun LogBuffer.shown(): List<String> = visibleSnapshot().map { it.message }

    @Test
    fun `context shows lines around each match, once, and marks them`() {
        val buffer = LogBuffer(capacity = 10_000)
        buffer.refilter(predicate = { it.message.startsWith(prefix = "hit") }, context = 1)
        buffer.append(entries = entries("a", "b", "hit1", "c", "hit2", "d", "e", "f"))

        assertEquals(listOf("b", "hit1", "c", "hit2", "d"), buffer.shown())
        assertEquals(setOf(2L, 4L, 6L), buffer.contextIdsSnapshot())
    }

    @Test
    fun `refilter with context works on lines already in the buffer`() {
        val buffer = LogBuffer(capacity = 10_000)
        buffer.append(entries = entries("a", "b", "hit", "c", "d"))

        buffer.refilter(predicate = { it.message == "hit" }, context = 2)

        assertEquals(listOf("a", "b", "hit", "c", "d"), buffer.shown())
        buffer.refilter(predicate = { it.message == "hit" }, context = 0)
        assertEquals(listOf("hit"), buffer.shown())
        assertEquals(emptySet<Long>(), buffer.contextIdsSnapshot())
    }

    @Test
    fun `the context term is parsed and does not filter`() {
        val matcher = LogFilterMatcher(filter = LogFilter(query = "context:3 hit "), processNames = emptyMap())

        assertEquals(3, matcher.contextLines)
        assertEquals(true, matcher.isQueryValid)
    }
}
