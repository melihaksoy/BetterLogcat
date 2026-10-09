package com.melihaksoy.betterlogcat.adb

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import java.nio.charset.StandardCharsets

/**
 * Incremental parser for `logcat -B` output (struct logger_entry, little endian).
 *
 * Binary output keeps exact message boundaries, so a multi-line message stays one entry.
 * Feed it raw chunks in any size; incomplete entries wait for the next chunk.
 */
class LogcatBinaryParser(private val nextId: () -> Long) {

    private var buffer = ByteArray(128 * 1024)
    private var size = 0

    fun feed(chunk: ByteArray): List<LogEntry> {
        ensureCapacity(extra = chunk.size)
        System.arraycopy(chunk, 0, buffer, size, chunk.size)
        size += chunk.size

        val entries = ArrayList<LogEntry>()
        var position = 0
        while (size - position >= MIN_HEADER) {
            val payloadLength = u16(offset = position)
            val declaredHeader = u16(offset = position + 2)
            val headerSize = if (declaredHeader == 0) V1_HEADER else declaredHeader
            if (headerSize < V1_HEADER || headerSize > MAX_HEADER) {
                // Lost sync. Skip one byte and try again.
                position += 1
                continue
            }
            val total = headerSize + payloadLength
            if (size - position < total) break

            val logId = if (headerSize >= 24) i32(offset = position + 20) else LOG_ID_MAIN
            if (logId !in BINARY_LOG_IDS && payloadLength > 0) {
                parseEntry(
                    start = position,
                    headerSize = headerSize,
                    payloadLength = payloadLength,
                )?.let { entries += it }
            }
            position += total
        }
        compact(consumed = position)
        return entries
    }

    private fun parseEntry(
        start: Int,
        headerSize: Int,
        payloadLength: Int,
    ): LogEntry? {
        val payloadStart = start + headerSize
        val payloadEnd = payloadStart + payloadLength
        val priority = buffer[payloadStart].toInt() and 0xFF
        val tagStart = payloadStart + 1
        val tagEnd = indexOfZero(
            from = tagStart,
            until = payloadEnd,
        )
        if (tagEnd < 0) return null
        val messageStart = tagEnd + 1
        val messageEnd = indexOfZero(
            from = messageStart,
            until = payloadEnd,
        ).let { if (it < 0) payloadEnd else it }

        return LogEntry(
            id = nextId(),
            epochSeconds = u32(offset = start + 12),
            nanos = i32(offset = start + 16),
            pid = i32(offset = start + 4),
            tid = i32(offset = start + 8),
            level = LogLevel.fromPriority(priority = priority),
            tag = decode(
                from = tagStart,
                until = tagEnd,
            ),
            message = decode(
                from = messageStart,
                until = messageEnd,
            ).trimEnd('\n', '\r'),
        )
    }

    private fun decode(
        from: Int,
        until: Int,
    ): String = if (until <= from) "" else String(buffer, from, until - from, StandardCharsets.UTF_8)

    private fun indexOfZero(
        from: Int,
        until: Int,
    ): Int {
        for (index in from until until) {
            if (buffer[index] == ZERO) return index
        }
        return -1
    }

    private fun u16(offset: Int): Int =
        (buffer[offset].toInt() and 0xFF) or ((buffer[offset + 1].toInt() and 0xFF) shl 8)

    private fun i32(offset: Int): Int =
        (buffer[offset].toInt() and 0xFF) or
            ((buffer[offset + 1].toInt() and 0xFF) shl 8) or
            ((buffer[offset + 2].toInt() and 0xFF) shl 16) or
            ((buffer[offset + 3].toInt() and 0xFF) shl 24)

    private fun u32(offset: Int): Long = i32(offset = offset).toLong() and 0xFFFFFFFFL

    private fun ensureCapacity(extra: Int) {
        if (size + extra <= buffer.size) return
        var newSize = buffer.size * 2
        while (newSize < size + extra) newSize *= 2
        buffer = buffer.copyOf(newSize = newSize)
    }

    private fun compact(consumed: Int) {
        if (consumed == 0) return
        System.arraycopy(buffer, consumed, buffer, 0, size - consumed)
        size -= consumed
    }

    private companion object {
        const val ZERO: Byte = 0
        const val MIN_HEADER = 4
        const val V1_HEADER = 20
        const val MAX_HEADER = 100
        const val LOG_ID_MAIN = 0

        /** events, stats and security buffers carry binary payloads, not text. */
        val BINARY_LOG_IDS = setOf(2, 5, 6)
    }
}
