package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.ui.logcat.component.KeywordHighlighter
import com.melihaksoy.betterlogcat.ui.logcat.component.MessageLabels
import com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter
import com.melihaksoy.betterlogcat.ui.logcat.component.buildMessageText
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorJsonTest {

    private val body = """{"results":[{"name":"bulbasaur","url":"https:\/\/pokeapi.co\/api\/v2\/pokemon\/1\/"}]}"""

    private val response = """
        RESPONSE: 200
        METHOD: GET
        FROM: https://pokeapi.co/api/v2/pokemon?limit=20
        COMMON HEADERS
        -> cache-control: public, max-age=86400
        -> x-trace: {"span":"a1b2c3","sampled":true}
        -> x-rate-limit: {"limit":100,"remaining":99,"reset":60}
        BODY Content-Type: application/json
        BODY START
        $body
        BODY END
    """.trimIndent()

    @Test
    fun `the body is the main part and header values stay inline`() {
        val json = JsonFormatter.detect(message = response) as JsonContent.Found

        assertEquals(3, json.blocks.size)
        assertEquals(body, json.main.raw)
        assertFalse(json.blocks.first { it.raw == body }.isInline)
        assertTrue(json.blocks.filter { it.raw != body }.all { it.isInline })
    }

    @Test
    fun `a body cut off by the logger still counts as json`() {
        val cut = response.substringBefore("\"url\"")
        val json = JsonFormatter.detect(message = cut) as JsonContent.Found

        assertFalse(json.main.isComplete)
        assertTrue(json.main.raw.startsWith("{\"results\""))
    }

    @Test
    fun `collapsed response shows the request address and the body preview`() {
        val text = render(isExpanded = false).text

        assertTrue(text, text.startsWith("RESPONSE: 200"))
        assertTrue(text, text.contains("https://pokeapi.co/api/v2/pokemon?limit=20"))
        assertTrue(text, text.contains("{\"results\""))
        assertFalse(text, text.contains("sampled"))
    }

    @Test
    fun `expanded response pretty prints only the body`() {
        val text = render(isExpanded = true).text

        assertTrue(text, text.contains("-> x-trace: {\"span\":\"a1b2c3\""))
        assertTrue(text, text.contains("BODY START\n{\n  \"results\": ["))
        assertTrue(text, text.contains("}\nBODY END"))
    }

    @Test
    fun `huge expanded messages are capped with a show all link`() {
        val huge = (1..5_000).joinToString(separator = "\n") { "line $it" }
        val text = buildMessageText(
            entry = entry(message = huge),
            isExpanded = true,
            prettyJson = true,
            isFullyShown = false,
            textColor = Color.White,
            colors = LogColors.Dark,
            highlighter = SearchHighlighter.None,
            keywords = KeywordHighlighter.None,
            labels = labels,
            onOpenFrame = {},
            onOpenUrl = {},
            onShowAll = {},
        ).text

        assertTrue(text.lines().size < 2_100)
        assertTrue(text.endsWith("Show all 5000 lines"))
    }

    private val labels = MessageLabels(
        showAll = { lines, _ -> "Show all $lines lines" },
    )

    private fun render(isExpanded: Boolean) = buildMessageText(
        entry = entry(message = response),
        isExpanded = isExpanded,
        prettyJson = true,
        isFullyShown = false,
        textColor = Color.White,
        colors = LogColors.Dark,
        highlighter = SearchHighlighter.None,
        keywords = KeywordHighlighter.None,
        labels = labels,
        onOpenFrame = {},
        onOpenUrl = {},
        onShowAll = {},
    )

    private fun entry(message: String) = LogEntry(
        id = 1,
        epochSeconds = 1_791_540_000L,
        nanos = 0,
        pid = 3776,
        tid = 4977,
        level = LogLevel.INFO,
        tag = "Ktor Client",
        message = message,
    )
}
