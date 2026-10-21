package com.melihaksoy.betterlogcat.ide

import com.melihaksoy.betterlogcat.format.SavedLogCodec
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.SavedLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files

class LastLogStoreTest {

    private val directory = Files.createTempDirectory("last-log")
    private val store = LastLogStore(directory = directory)

    @Test
    fun `a saved log reads back with exact times and new ids`() {
        val log = log(
            serial = "192.168.1.20:5555",
            entry(id = 40, nanos = 123_456_789, message = "line one\n  line two"),
            entry(id = 41, nanos = 123_456_790, message = "ünïcödé ✓ " + "x".repeat(n = 70_000)),
        )
        store.save(log = log)

        val loaded = store.load(serial = "192.168.1.20:5555", firstId = 1L)!!

        assertEquals(listOf(1L, 2L), loaded.entries.map { it.id })
        assertEquals(log.entries.map { it.message }, loaded.entries.map { it.message })
        assertEquals(listOf(123_456_789, 123_456_790), loaded.entries.map { it.nanos })
        assertEquals(LogLevel.ERROR, loaded.entries.first().level)
        assertEquals("Pixel 9", loaded.deviceName)
        assertEquals(mapOf(PID to "com.example.app"), loaded.processNames)
    }

    @Test
    fun `each device has its own file and delete removes only its own`() {
        store.save(log = log(serial = "emulator-5554", entry(id = 1, nanos = 0, message = "a")))
        store.save(log = log(serial = "emulator-5556", entry(id = 1, nanos = 0, message = "b")))

        store.delete(serial = "emulator-5554")

        assertNull(store.load(serial = "emulator-5554", firstId = 1L))
        assertEquals("b", store.load(serial = "emulator-5556", firstId = 1L)!!.entries.single().message)
    }

    @Test
    fun `a broken file reads as nothing and is deleted`() {
        store.save(log = log(serial = "emulator-5554", entry(id = 1, nanos = 0, message = "a")))
        Files.list(directory).use { files -> files.forEach { Files.write(it, byteArrayOf(1, 2, 3)) } }

        assertNull(store.load(serial = "emulator-5554", firstId = 1L))
        assertEquals(0L, Files.list(directory).use { it.count() })
    }

    @Test(expected = IOException::class)
    fun `the codec rejects another format`() {
        val bytes = ByteArrayOutputStream().also { it.write(byteArrayOf(0, 0, 0, 1, 0, 0, 0, 1)) }.toByteArray()
        SavedLogCodec.read(
            input = ByteArrayInputStream(bytes),
            firstId = 1L,
        )
    }

    private fun log(
        serial: String,
        vararg entries: LogEntry,
    ) = SavedLog(
        serial = serial,
        deviceName = "Pixel 9",
        entries = entries.toList(),
        processNames = mapOf(PID to "com.example.app"),
    )

    private fun entry(
        id: Long,
        nanos: Int,
        message: String,
    ) = LogEntry(
        id = id,
        epochSeconds = 1_791_540_000L,
        nanos = nanos,
        pid = PID,
        tid = PID,
        level = if (id % 2 == 0L) LogLevel.ERROR else LogLevel.INFO,
        tag = "Garage",
        message = message,
    )

    private companion object {
        const val PID = 14_302
    }
}
