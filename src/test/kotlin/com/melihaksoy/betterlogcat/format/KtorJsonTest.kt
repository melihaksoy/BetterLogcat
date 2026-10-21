package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.ui.logcat.component.JsonPath
import com.melihaksoy.betterlogcat.ui.logcat.component.KeywordHighlighter
import com.melihaksoy.betterlogcat.ui.logcat.component.LogListLayout
import com.melihaksoy.betterlogcat.ui.logcat.component.MessageLabels
import com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter
import com.melihaksoy.betterlogcat.ui.logcat.component.buildMessageText
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
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
        assertTrue(text, text.contains("GET https://pokeapi.co/api/v2/pokemon?limit=20"))
        assertTrue(text, text.contains("{\"results\""))
        assertFalse(text, text.contains("sampled"))
    }

    @Test
    fun `collapsed request shows the method before the address`() {
        val request = "REQUEST: https://pokeapi.co/api/v2/pokemon\nMETHOD: HttpMethod(value=POST)\nBODY START\n{\"name\":\"pikachu\"}\nBODY END"
        val text = buildMessageText(
            entry = entry(message = request),
            isExpanded = false,
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

        assertTrue(text, text.startsWith("REQUEST: POST https://pokeapi.co/api/v2/pokemon"))
    }

    @Test
    fun `expanded response pretty prints only the body`() {
        val text = render(isExpanded = true).text

        assertTrue(text, text.contains("-> x-trace: {\"span\":\"a1b2c3\""))
        assertTrue(text, text.contains("BODY START\n▾ {\n  ▾ \"results\": ["))
        assertTrue(text, text.contains("  }\nBODY END"))
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

    @Test
    fun `collapsed response shows how long the call took`() {
        val call = NetworkCall(
            method = "GET",
            url = "https://pokeapi.co/api/v2/pokemon?limit=20",
            requestEntryId = 0,
            requestEpochMillis = 0,
            responseEntryId = 1,
            statusCode = 200,
            durationMillis = 340,
        )
        val text = render(
            isExpanded = false,
            call = call,
        ).text

        assertTrue(text, text.startsWith("RESPONSE: 200  ·  340 ms  ·  GET https://pokeapi.co"))
    }

    @Test
    fun `a folded block shows its size and its click unfolds it`() {
        var toggled = -1
        val unfolded = render(isExpanded = true).text
        val folded = render(
            isExpanded = true,
            jsonFolds = setOf(0),
            onToggleJsonFold = { toggled = it },
        )

        assertTrue(unfolded, unfolded.contains("\"name\": \"bulbasaur\""))
        assertTrue(folded.text, folded.text.contains("BODY START\n▸ { … 1 key }\nBODY END"))
        val link = folded.getLinkAnnotations(start = 0, end = folded.length).first { (it.item as? LinkAnnotation.Clickable)?.tag == "fold" }
        (link.item as LinkAnnotation.Clickable).linkInteractionListener?.onClick(link.item)
        assertEquals(0, toggled)
    }

    @Test
    fun `a line knows the json blocks around it`() {
        val text = render(isExpanded = true)
        val lines = text.text.lines()
        val nameLine = lines.indexOfFirst { it.contains("\"name\": \"bulbasaur\"") }

        val path = JsonPath.at(
            text = text,
            line = nameLine,
        )

        assertEquals(listOf("{ }", "results", "[0]"), path.map { it.label })
        val item = JsonPath.find(
            pieces = LogListLayout.split(
                text = text,
                linesPerPiece = 4,
            ),
            block = path.last().block,
        )
        assertEquals(nameLine - 1, item!!.piece * 4 + item.line)
        assertEquals("    ▾ {", lines[nameLine - 1])
    }

    @Test
    fun `a folded block is still in the path of the lines around it`() {
        val text = render(
            isExpanded = true,
            jsonFolds = setOf(2),
        )
        val itemLine = text.text.lines().indexOfFirst { it.contains("{ … 2 keys }") }

        val path = JsonPath.at(
            text = text,
            line = itemLine,
        )

        assertEquals(listOf("{ }", "results", "[0]"), path.map { it.label })
    }

    private val labels = MessageLabels(
        showAll = { lines, _ -> "Show all $lines lines" },
        duration = { "$it ms" },
        folded = { count, isArray -> if (isArray) "$count items" else if (count == 1) "$count key" else "$count keys" },
    )

    private fun render(
        isExpanded: Boolean,
        call: NetworkCall? = null,
        jsonFolds: Set<Int> = emptySet(),
        onToggleJsonFold: (Int) -> Unit = {},
    ) = buildMessageText(
        entry = entry(message = response),
        isExpanded = isExpanded,
        prettyJson = true,
        isFullyShown = false,
        textColor = Color.White,
        colors = LogColors.Dark,
        highlighter = SearchHighlighter.None,
        keywords = KeywordHighlighter.None,
        call = call,
        jsonFolds = jsonFolds,
        labels = labels,
        onOpenFrame = {},
        onOpenUrl = {},
        onShowAll = {},
        onToggleJsonFold = onToggleJsonFold,
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
