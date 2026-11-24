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

    @Test
    fun `errors follow their own default before JSON and multi-line`() {
        val options = ViewOptionsUiModel(
            expandByDefault = false,
            expandJsonByDefault = false,
            expandErrorsByDefault = true,
        )
        val trace = entry(
            message = "java.lang.IllegalStateException: boom\n\tat com.example.Main.run(Main.kt:3)",
            level = LogLevel.ERROR,
        )
        val jsonError = entry(
            message = "failed: {\"code\": 500, \"error\": \"server\"}",
            level = LogLevel.ERROR,
        )

        assertTrue(
            options.isExpanded(
                entry = trace,
                isToggled = false,
            ),
        )
        assertTrue(
            options.isExpanded(
                entry = jsonError,
                isToggled = false,
            ),
        )
        assertFalse(
            options.isExpanded(
                entry = trace,
                isToggled = true,
            ),
        )
        assertFalse(
            options.isExpanded(
                entry = entry(message = "first\nsecond"),
                isToggled = false,
            ),
        )
    }

    @Test
    fun `HTTP calls follow their own default before JSON`() {
        val response = entry(message = "RESPONSE: 200\nMETHOD: GET\nBODY START\n{\"id\": 2}\nBODY END")
        val plainJson = entry(message = "state: {\"id\": 2, \"name\": \"item\"}")
        val options = ViewOptionsUiModel(
            expandJsonByDefault = false,
            expandNetworkByDefault = true,
        )

        assertTrue(
            options.isExpanded(
                entry = response,
                isToggled = false,
            ),
        )
        assertFalse(
            options.isExpanded(
                entry = plainJson,
                isToggled = false,
            ),
        )
        assertFalse(
            options.with(
                toggle = ViewToggle.EXPAND_NETWORK,
                isOn = false,
            ).isExpanded(
                entry = response,
                isToggled = false,
            ),
        )
    }

    @Test
    fun `expand all sets every default`() {
        val collapsed = ViewOptionsUiModel().withAllExpanded(isExpanded = false)

        ViewToggle.entries
            .filter { it.name.startsWith("EXPAND_") }
            .forEach { toggle -> assertFalse(collapsed.isOn(toggle = toggle)) }
        ViewToggle.entries
            .filter { it.name.startsWith("EXPAND_") }
            .forEach { toggle -> assertTrue(collapsed.withAllExpanded(isExpanded = true).isOn(toggle = toggle)) }
    }

    private fun entry(
        message: String,
        level: LogLevel = LogLevel.DEBUG,
    ) = LogEntry(
        id = 1,
        epochSeconds = 1_791_540_000L,
        nanos = 0,
        pid = 100,
        tid = 100,
        level = level,
        tag = "SatelliteController",
        message = message,
    )
}
