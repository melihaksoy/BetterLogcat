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

    private val body = """{"data":[{"id":"C2050077","vehicle":{"make":{"name":"Volkswagen"}},"thumbnail":"https:\/\/assets.example.com\/uploads\/1.jpg"}]}"""

    private val response = """
        RESPONSE: 200
        METHOD: GET
        FROM: https://staging.example.com/app-api/v1/search/top-picks
        COMMON HEADERS
        -> cache-control: no-cache, private
        -> nel: {"report_to":"cf-nel","success_fraction":0.0,"max_age":604800}
        -> report-to: {"group":"cf-nel","max_age":604800,"endpoints":[{"url":"https:\/\/a.nel.cloudflare.com\/report"}]}
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
        val cut = response.substringBefore("\"thumbnail\"")
        val json = JsonFormatter.detect(message = cut) as JsonContent.Found

        assertFalse(json.main.isComplete)
        assertTrue(json.main.raw.startsWith("{\"data\""))
    }

    @Test
    fun `collapsed response shows the request address and the body preview`() {
        val text = render(isExpanded = false).text

        assertTrue(text, text.startsWith("RESPONSE: 200"))
        assertTrue(text, text.contains("https://staging.example.com/app-api/v1/search/top-picks"))
        assertTrue(text, text.contains("{\"data\""))
        assertFalse(text, text.contains("report_to"))
    }

    @Test
    fun `expanded response pretty prints only the body`() {
        val text = render(isExpanded = true).text

        assertTrue(text, text.contains("-> nel: {\"report_to\":\"cf-nel\""))
        assertTrue(text, text.contains("BODY START\n{\n  \"data\": ["))
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
