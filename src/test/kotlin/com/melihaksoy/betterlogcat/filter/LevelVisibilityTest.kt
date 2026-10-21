package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelVisibilityTest {

    private val all = LogLevel.entries.toSet()

    @Test
    fun `hiding the lowest levels writes a min level term`() {
        var filter = LogFilter().withQueryString(text = "package:mine timeout")
        filter = LevelVisibility.toggle(filter = filter, level = LogLevel.VERBOSE)
        filter = LevelVisibility.toggle(filter = filter, level = LogLevel.DEBUG)

        assertEquals("package:mine level:I timeout ", filter.query)
        assertEquals(setOf(LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.ASSERT), LevelVisibility.visible(filter = filter))
    }

    @Test
    fun `a gap writes the shorter list`() {
        val hideInfo = LevelVisibility.toggle(filter = LogFilter(), level = LogLevel.INFO)
        val onlyErrors = LevelVisibility.withVisible(filter = LogFilter(), levels = setOf(LogLevel.DEBUG, LogLevel.ERROR))

        assertEquals("-level=:I ", hideInfo.query)
        assertEquals(all - LogLevel.INFO, LevelVisibility.visible(filter = hideInfo))
        assertEquals("level=:D,E ", onlyErrors.query)
        assertEquals(setOf(LogLevel.DEBUG, LogLevel.ERROR), LevelVisibility.visible(filter = onlyErrors))
    }

    @Test
    fun `a typed level term drives the toggles`() {
        val filter = LogFilter().withQueryString(text = "level:warn")

        assertEquals(setOf(LogLevel.WARN, LogLevel.ERROR, LogLevel.ASSERT), LevelVisibility.visible(filter = filter))
        assertEquals("level:W ", LevelVisibility.toggle(filter = filter, level = LogLevel.ASSERT).let { LevelVisibility.toggle(filter = it, level = LogLevel.ASSERT) }.query)
    }

    @Test
    fun `turning every level back on removes the term`() {
        val hidden = LevelVisibility.toggle(filter = LogFilter().withQueryString(text = "timeout"), level = LogLevel.VERBOSE)
        val shown = LevelVisibility.toggle(filter = hidden, level = LogLevel.VERBOSE)

        assertEquals("timeout ", shown.query)
        assertEquals(all, LevelVisibility.visible(filter = shown))
    }

    @Test
    fun `the last level stays on`() {
        val filter = LogFilter().withQueryString(text = "level:A")

        assertEquals(filter, LevelVisibility.toggle(filter = filter, level = LogLevel.ASSERT))
    }

    @Test
    fun `old level toggles move into the text`() {
        val filter = LevelVisibility.withVisible(
            filter = LogFilter(levels = setOf(LogLevel.ERROR, LogLevel.ASSERT)),
            levels = setOf(LogLevel.ERROR, LogLevel.ASSERT),
        )

        assertEquals(all, filter.levels)
        assertEquals("level:E ", filter.query)
    }
}
