package com.melihaksoy.betterlogcat.adb

import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LogcatBinaryParserTest {

    private var nextId = 0L
    private val parser = LogcatBinaryParser(nextId = { ++nextId })

    @Test
    fun `parses one v4 entry`() {
        val bytes = entry(
            priority = 6,
            tag = "AndroidRuntime",
            message = "FATAL EXCEPTION: main\n",
        )

        val entries = parser.feed(chunk = bytes)

        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals(LogLevel.ERROR, entry.level)
        assertEquals("AndroidRuntime", entry.tag)
        assertEquals("FATAL EXCEPTION: main", entry.message)
        assertEquals(4242, entry.pid)
        assertEquals(4250, entry.tid)
        assertEquals(1_791_540_000L, entry.epochSeconds)
        assertEquals(123_456_789, entry.nanos)
    }

    @Test
    fun `keeps a multi-line message as one entry`() {
        val trace = "java.lang.IllegalStateException: boom\n\tat com.example.Foo.bar(Foo.kt:12)\n\tat com.example.Main.run(Main.kt:3)"
        val entries = parser.feed(
            chunk = entry(
                priority = 6,
                tag = "AndroidRuntime",
                message = trace,
            ),
        )

        assertEquals(1, entries.size)
        assertEquals(3, entries.single().lines.size)
    }

    @Test
    fun `waits for the rest of an entry split across chunks`() {
        val bytes = entry(
            priority = 4,
            tag = "Garage",
            message = "Cold start finished",
        ) + entry(
            priority = 3,
            tag = "OkHttp",
            message = "--> GET /api/v2/pokemon",
        )

        val first = parser.feed(chunk = bytes.copyOfRange(0, 7))
        val second = parser.feed(chunk = bytes.copyOfRange(7, bytes.size - 3))
        val third = parser.feed(chunk = bytes.copyOfRange(bytes.size - 3, bytes.size))

        assertTrue(first.isEmpty())
        assertEquals(listOf("Garage"), second.map { it.tag })
        assertEquals(listOf("OkHttp"), third.map { it.tag })
    }

    @Test
    fun `skips binary event buffer entries`() {
        val bytes = entry(
            priority = 4,
            tag = "am_proc_start",
            message = "x",
            logId = 2,
        ) + entry(
            priority = 4,
            tag = "Garage",
            message = "visible",
        )

        val entries = parser.feed(chunk = bytes)

        assertEquals(listOf("Garage"), entries.map { it.tag })
    }

    @Test
    fun `reads v1 entries without header size`() {
        val entries = parser.feed(
            chunk = entry(
                priority = 5,
                tag = "Legacy",
                message = "old device",
                headerSize = 0,
            ),
        )

        assertEquals(LogLevel.WARN, entries.single().level)
        assertEquals("old device", entries.single().message)
    }

    @Test
    fun `assigns increasing ids`() {
        val bytes = entry(
            priority = 4,
            tag = "A",
            message = "1",
        ) + entry(
            priority = 4,
            tag = "B",
            message = "2",
        )

        val ids = parser.feed(chunk = bytes).map { it.id }

        assertEquals(listOf(1L, 2L), ids)
    }

    private fun entry(
        priority: Int,
        tag: String,
        message: String,
        logId: Int = 0,
        headerSize: Int = 28,
    ): ByteArray {
        val payload = ByteArrayOutputStream()
        payload.write(priority)
        payload.write(tag.toByteArray())
        payload.write(0)
        payload.write(message.toByteArray())
        payload.write(0)
        val payloadBytes = payload.toByteArray()

        val header = ByteBuffer.allocate(if (headerSize == 0) 20 else headerSize).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(payloadBytes.size.toShort())
        header.putShort(headerSize.toShort())
        header.putInt(4242)
        header.putInt(4250)
        header.putInt(1_791_540_000)
        header.putInt(123_456_789)
        if (headerSize >= 24) header.putInt(logId)
        if (headerSize >= 28) header.putInt(10_123)
        return header.array() + payloadBytes
    }
}
