package com.melihaksoy.betterlogcat.ui.logcat.model

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewOptionsUiModelTest {

    private val longLine = entry(message = "config= PersistableBundle[{" + "satellite_flag=false, ".repeat(n = 60) + "}]")

    @Test
    fun `long line expands with soft wrap`() {
        val options = ViewOptionsUiModel(
            softWrap = true,
            expandJsonByDefault = false,
        )

        assertTrue(options.isExpandable(entry = longLine))
        assertFalse(
            options.isExpanded(
                entry = longLine,
                isToggled = false,
            ),
        )
    }

    @Test
    fun `long line without soft wrap shows whole and has no toggle`() {
        val options = ViewOptionsUiModel(
            softWrap = false,
            expandJsonByDefault = false,
        )

        assertFalse(options.isExpandable(entry = longLine))
        assertTrue(
            options.isExpanded(
                entry = longLine,
                isToggled = false,
            ),
        )
        assertTrue(
            options.isExpanded(
                entry = longLine,
                isToggled = true,
            ),
        )
    }

    @Test
    fun `several lines and JSON still expand without soft wrap`() {
        val options = ViewOptionsUiModel(softWrap = false)

        assertTrue(options.isExpandable(entry = entry(message = "first\nsecond")))
        assertTrue(options.isExpandable(entry = entry(message = "RESPONSE: {\"id\": 2, \"name\": \"item\"}")))
    }

    @Test
    fun `short line never expands`() {
        val options = ViewOptionsUiModel(softWrap = false)
        val short = entry(message = "short")

        assertFalse(options.isExpandable(entry = short))
        assertFalse(
            options.isExpanded(
                entry = short,
                isToggled = true,
            ),
        )
    }

    private fun entry(message: String) = LogEntry(
        id = 1,
        epochSeconds = 1_791_540_000L,
        nanos = 0,
        pid = 100,
        tid = 100,
        level = LogLevel.DEBUG,
        tag = "SatelliteController",
        message = message,
    )
}
