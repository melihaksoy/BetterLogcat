package com.melihaksoy.betterlogcat.network

import com.melihaksoy.betterlogcat.filter.LogBuffer
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.ProcessEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkTrackerTest {

    @Test
    fun `ktor request and response pair up`() {
        val tracker = NetworkTracker()
        tracker.onEntry(entry = entry(id = 1, millis = 0, message = "REQUEST: https://api.example.com/v1/search/top-picks\nMETHOD: GET\nCOMMON HEADERS"))
        tracker.onEntry(entry = entry(id = 2, millis = 340, message = "RESPONSE: 200\nMETHOD: GET\nFROM: https://api.example.com/v1/search/top-picks\nBODY START\n{}\nBODY END"))

        val call = tracker.snapshot().single()
        assertEquals("GET", call.method)
        assertEquals(200, call.statusCode)
        assertEquals(340, call.durationMillis)
        assertEquals(1, call.requestEntryId)
        assertEquals(2, call.responseEntryId)
    }

    @Test
    fun `okhttp lines pair up and keep the logged duration`() {
        val tracker = NetworkTracker()
        tracker.onEntry(entry = entry(id = 1, millis = 0, message = "--> POST https://api.example.com/v1/login"))
        tracker.onEntry(entry = entry(id = 2, millis = 900, message = "<-- 401 Unauthorized https://api.example.com/v1/login (123ms)"))

        val call = tracker.snapshot().single()
        assertEquals(401, call.statusCode)
        assertEquals(123, call.durationMillis)
    }

    @Test
    fun `an open request stays pending`() {
        val tracker = NetworkTracker()
        tracker.onEntry(entry = entry(id = 1, millis = 0, message = "--> GET https://api.example.com/slow"))

        assertTrue(tracker.snapshot().single().isPending)
    }

    @Test
    fun `curl from a ktor request`() {
        val curl = CurlBuilder.fromMessage(
            message = """
                REQUEST: https://api.example.com/v1/items
                METHOD: POST
                COMMON HEADERS
                -> Accept: application/json
                -> Authorization: Bearer abc
                CONTENT HEADERS
                -> Content-Length: 13
                BODY Content-Type: application/json
                BODY START
                {"name":"it's"}
                BODY END
            """.trimIndent(),
        )

        assertEquals(
            "curl -X POST 'https://api.example.com/v1/items' \\\n" +
                "  -H 'Accept: application/json' \\\n" +
                "  -H 'Authorization: Bearer abc' \\\n" +
                "  --data-raw '{\"name\":\"it'\\''s\"}'",
            curl,
        )
    }

    @Test
    fun `process start and death lines`() {
        assertEquals(
            ProcessEvent.Started(packageName = "com.example.app", pid = 15011),
            ProcessEvent.parse(tag = "ActivityManager", message = "Start proc 15011:com.example.app/u0a215 for next-top-activity {com.example.app/.MainActivity}"),
        )
        assertEquals(
            ProcessEvent.Died(packageName = "com.example.app", pid = 14302),
            ProcessEvent.parse(tag = "ActivityManager", message = "Process com.example.app (pid 14302) has died: fg  TOP"),
        )
        assertNull(ProcessEvent.parse(tag = "OkHttp", message = "Start proc 1:x/y"))
    }

    @Test
    fun `tag rates count entries per second`() {
        val buffer = LogBuffer(capacity = 10_000)
        buffer.append(entries = (1L..5L).map { entry(id = it, millis = 0, message = "a") } + (6L..7L).map { entry(id = it, millis = 1_000, message = "b") })

        val rates = buffer.tagRatesSnapshot().getValue("Ktor Client")
        assertEquals(30, rates.size)
        assertEquals(2, rates.last())
        assertEquals(5, rates[rates.size - 2])
    }

    private fun entry(
        id: Long,
        millis: Long,
        message: String,
    ) = LogEntry(
        id = id,
        epochSeconds = 1_791_540_000L + millis / 1_000,
        nanos = (millis % 1_000 * 1_000_000).toInt(),
        pid = 3776,
        tid = (4_000 + id).toInt(),
        level = LogLevel.INFO,
        tag = "Ktor Client",
        message = message,
    )
}
