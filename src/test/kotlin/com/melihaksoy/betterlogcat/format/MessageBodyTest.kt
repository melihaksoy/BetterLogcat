package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageBodyTest {

    private val body = """{"results":[{"name":"bulbasaur","id":1}]}"""

    private fun response(body: String) = """
        RESPONSE: 200
        METHOD: GET
        FROM: https://pokeapi.co/api/v2/pokemon?limit=20
        COMMON HEADERS
        -> cache-control: public, max-age=86400
        -> x-trace: {"span":"a1b2c3","sampled":true}
        BODY Content-Type: application/json
        BODY START
        $body
        BODY END
    """.trimIndent()

    @Test
    fun `body is the pretty JSON alone`() {
        val copied = MessageDocument.body(entry = entry(message = response(body = body)))

        assertTrue(copied!!.startsWith("{\n"))
        assertTrue(copied.endsWith("}"))
        assertTrue(copied.contains("\"name\": \"bulbasaur\""))
        assertFalse(copied.contains("RESPONSE"))
        assertFalse(copied.contains("BODY"))
        assertFalse(copied.contains("cache-control"))
        assertFalse(copied.contains('▾'))
    }

    @Test
    fun `body cut off by the logger comes back as logged`() {
        val cut = response(body = body).substringBefore(",\"id\"")
        val copied = MessageDocument.body(entry = entry(message = cut))

        assertTrue(copied!!.startsWith("{\"results\""))
        assertFalse(copied.contains("BODY"))
    }

    @Test
    fun `JSON only inside a header is not a body`() {
        val copied = MessageDocument.body(entry = entry(message = response(body = "<html>ok</html>")))

        assertNull(copied)
    }

    @Test
    fun `message without JSON has no body`() {
        assertNull(MessageDocument.body(entry = entry(message = "RESPONSE: 204\nMETHOD: GET")))
    }

    private fun entry(message: String) = LogEntry(
        id = 1,
        epochSeconds = 1_791_540_000L,
        nanos = 0,
        pid = 100,
        tid = 100,
        level = LogLevel.INFO,
        tag = "HttpClient",
        message = message,
    )
}
