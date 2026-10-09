package com.melihaksoy.betterlogcat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightPresetTest {

    private val response = """
        RESPONSE: 200
        METHOD: GET
        FROM: https://staging.example.com/app-api/v1/search/listings?listing_type_ex=auction
        COMMON HEADERS
        -> cache-control: no-cache, private
        -> content-type: application/json
        -> x-frame-options: SAMEORIGIN
        BODY Content-Type: application/json
        BODY START
        {"data":[{"id":"C1871320","status":"ok"}]}
        BODY END
    """.trimIndent()

    private val request = """
        REQUEST: https://api.example.com/v1/items
        METHOD: HttpMethod(value=POST)
        CONTENT HEADERS
        -> Content-Length: 12
    """.trimIndent()

    private fun matches(
        color: Long,
        text: String,
    ): List<String> = HighlightPreset.Ktor.rules
        .filter { it.colorArgb == color }
        .flatMap { rule -> Regex(pattern = rule.pattern).findAll(input = text).map { it.value }.toList() }

    @Test
    fun `status codes get their class color`() {
        assertEquals(listOf("200"), matches(color = 0xFF2EB67D, text = response))
        assertEquals(listOf("404 Not Found"), matches(color = 0xFFF39C12, text = "RESPONSE: 404 Not Found\nMETHOD: GET"))
        assertEquals(listOf("503"), matches(color = 0xFFE06C75, text = "RESPONSE: 503\nMETHOD: GET"))
        assertEquals(listOf("304 Not Modified"), matches(color = 0xFF61AFEF, text = "RESPONSE: 304 Not Modified"))
    }

    @Test
    fun `status stops before inline json`() {
        assertEquals(listOf("200"), matches(color = 0xFF2EB67D, text = "RESPONSE: 200 {\"data\":[]}"))
    }

    @Test
    fun `section labels and method`() {
        assertEquals(
            listOf("RESPONSE", "METHOD", "FROM", "COMMON HEADERS", "BODY Content-Type", "BODY START", "BODY END"),
            matches(color = 0xFFC678DD, text = response),
        )
        assertEquals(listOf("GET"), matches(color = 0xFF56B6C2, text = response))
        assertEquals(listOf("HttpMethod(value=POST)"), matches(color = 0xFF56B6C2, text = request))
    }

    @Test
    fun `header names only, not values`() {
        assertEquals(listOf("cache-control", "content-type", "x-frame-options"), matches(color = 0xFF7C83FD, text = response))
        assertEquals(listOf("Content-Length"), matches(color = 0xFF7C83FD, text = request))
    }

    @Test
    fun `json body and plain logs stay plain`() {
        val plain = "Basket loaded, status 200, METHOD: GET inside a sentence"
        HighlightPreset.Ktor.rules.forEach { rule ->
            assertFalse(rule.pattern, Regex(pattern = rule.pattern).containsMatchIn(input = plain))
            assertFalse(rule.pattern, Regex(pattern = rule.pattern).containsMatchIn(input = "{\"data\":[{\"id\":\"C1871320\",\"status\":\"ok\"}]}"))
        }
    }

    @Test
    fun `added once every pattern is in, recolored rules count`() {
        val rules = HighlightPreset.Ktor.rules
        assertFalse(HighlightPreset.Ktor.isAdded(current = rules.drop(1)))
        assertTrue(HighlightPreset.Ktor.isAdded(current = rules.map { it.copy(colorArgb = 0xFFFFFFFF) }))
    }
}
