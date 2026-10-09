package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuerySuggestionsTest {

    private val context = SuggestionContext(
        packages = listOf("com.example.app", "com.example.app.debug"),
        tags = listOf("OkHttp", "ActivityThread", "art"),
        processNames = listOf("com.example.app", "system_server"),
    )

    @Test
    fun `a bare word suggests keys`() {
        val list = suggest(text = "pa")

        assertEquals(listOf("package:", "package:mine"), list.items.map { it.label })
        assertEquals("package:", list.items.first().replacement)
        assertEquals("package:mine ", list.items.last().replacement)
    }

    @Test
    fun `a fully typed key stays suggested until the colon`() {
        assertEquals(listOf("tag:"), suggest(text = "tag").items.map { it.label })
        assertEquals("tag:", suggest(text = "-tag").items.single().replacement.removePrefix("-"))
    }

    @Test
    fun `an empty token lists every key`() {
        val list = suggest(text = "timeout ")

        assertTrue(list.isTokenEmpty)
        // Every key, plus the package:mine shortcut.
        assertEquals(QueryKey.entries.size + 1, list.items.size)
    }

    @Test
    fun `package values start with mine and end with a space`() {
        val list = suggest(text = "package:")

        assertEquals(listOf("mine", "com.example.app", "com.example.app.debug"), list.items.map { it.label })
        assertEquals("package:mine ", list.items.first().replacement)
    }

    @Test
    fun `values filter by prefix first then by contains and keep negation and mode`() {
        val list = suggest(text = "-tag~:t")

        assertEquals(listOf("OkHttp", "ActivityThread", "art"), list.items.map { it.label })
        assertEquals("-tag~:OkHttp ", list.items.first().replacement)
        assertEquals(0, list.tokenStart)
    }

    @Test
    fun `only the token under the cursor is replaced`() {
        val text = "timeout lev"
        val list = suggest(text = text)

        assertEquals(8, list.tokenStart)
        assertEquals(text.length, list.tokenEnd)
        assertEquals("level:", list.items.single().label)
    }

    @Test
    fun `a term still being typed already filters`() {
        val filter = LogFilter(query = "package:mine")
        val matcher = LogFilterMatcher(
            filter = filter.effective(),
            processNames = mapOf(1 to "com.example.app", 2 to "com.android.systemui"),
            projectAppIds = setOf("com.example.app"),
        )

        assertEquals("", filter.effective().query)
        assertTrue(matcher.matches(entry = entry(pid = 1)))
        assertTrue(!matcher.matches(entry = entry(pid = 2)))
    }

    private fun suggest(text: String) = QuerySuggestions.suggest(
        text = text,
        cursor = text.length,
        context = context,
    )

    private fun entry(pid: Int) = LogEntry(
        id = 1,
        epochSeconds = 1_791_540_000L,
        nanos = 0,
        pid = pid,
        tid = pid,
        level = LogLevel.INFO,
        tag = "Garage",
        message = "Basket loaded",
    )
}
