package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpTermsTest {

    private val ktorRequest = entry(message = "REQUEST: https://pokeapi.co/api/v2/pokemon\nMETHOD: HttpMethod(value=POST)")
    private val ktorResponse = entry(message = "RESPONSE: 404 Not Found\nMETHOD: GET\nFROM: https://pokeapi.co/api/v2/pokemon/0")
    private val okHttpRequest = entry(message = "--> DELETE https://pokeapi.co/api/v2/pokemon/1")
    private val okHttpResponse = entry(message = "<-- 503 Service Unavailable https://pokeapi.co/api/v2/pokemon/1 (87ms)")
    private val plain = entry(message = "Basket loaded, status 404, METHOD: GET")

    @Test
    fun `method matches requests and ktor responses`() {
        val matcher = matcher(query = "method:post")

        assertTrue(matcher.matches(entry = ktorRequest))
        assertFalse(matcher.matches(entry = okHttpRequest))
        assertTrue(matcher(query = "method:GET").matches(entry = ktorResponse))
        assertTrue(matcher(query = "method=:PUT,DELETE").matches(entry = okHttpRequest))
        assertFalse(matcher(query = "method:GET").matches(entry = plain))
    }

    @Test
    fun `status takes a code or a class`() {
        assertTrue(matcher(query = "status:404").matches(entry = ktorResponse))
        assertTrue(matcher(query = "status:4xx").matches(entry = ktorResponse))
        assertFalse(matcher(query = "status:5xx").matches(entry = ktorResponse))
        assertTrue(matcher(query = "status:5xx").matches(entry = okHttpResponse))
        assertTrue(matcher(query = "status=:404,503").matches(entry = okHttpResponse))
        assertFalse(matcher(query = "status:4xx").matches(entry = ktorRequest))
        assertFalse(matcher(query = "status:4xx").matches(entry = plain))
        assertTrue(matcher(query = "-status:4xx").matches(entry = okHttpResponse))
    }

    @Test
    fun `a bad status pattern marks the query invalid`() {
        assertFalse(matcher(query = "status:abc").isQueryValid)
        assertTrue(matcher(query = "status:2xx").isQueryValid)
    }

    @Test
    fun `is http finds every http line`() {
        val matcher = matcher(query = "is:http")

        assertEquals(
            listOf(true, true, true, true, false),
            listOf(ktorRequest, ktorResponse, okHttpRequest, okHttpResponse, plain).map { matcher.matches(entry = it) },
        )
    }

    private fun matcher(query: String) = LogFilterMatcher(
        filter = LogFilter().withQueryString(text = query),
        processNames = emptyMap(),
    )

    private fun entry(message: String) = LogEntry(
        id = 1,
        epochSeconds = 1_791_540_000L,
        nanos = 0,
        pid = 10,
        tid = 10,
        level = LogLevel.INFO,
        tag = "Ktor Client",
        message = message,
    )
}
