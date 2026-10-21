package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.SavedLog
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Binary form of a [SavedLog]. Unlike the Export text, it keeps the exact nanoseconds, so the live
 * stream can continue right after the last saved line without repeating it.
 */
object SavedLogCodec {

    fun write(
        log: SavedLog,
        output: OutputStream,
    ) {
        val data = DataOutputStream(output)
        data.writeInt(MAGIC)
        data.writeInt(VERSION)
        data.writeText(text = log.serial)
        data.writeText(text = log.deviceName)
        data.writeInt(log.processNames.size)
        log.processNames.forEach { (pid, name) ->
            data.writeInt(pid)
            data.writeText(text = name)
        }
        data.writeInt(log.entries.size)
        log.entries.forEach { entry ->
            data.writeLong(entry.epochSeconds)
            data.writeInt(entry.nanos)
            data.writeInt(entry.pid)
            data.writeInt(entry.tid)
            data.writeChar(entry.level.letter.code)
            data.writeText(text = entry.tag)
            data.writeText(text = entry.message)
        }
        data.flush()
    }

    /** Entries get ids from [firstId] up. Throws [IOException] for a file of another format or version. */
    fun read(
        input: InputStream,
        firstId: Long,
    ): SavedLog {
        val data = DataInputStream(input)
        if (data.readInt() != MAGIC || data.readInt() != VERSION) throw IOException("Not a saved log")
        val serial = data.readText()
        val deviceName = data.readText()
        val processNames = HashMap<Int, String>()
        repeat(times = data.readCount()) {
            processNames[data.readInt()] = data.readText()
        }
        val count = data.readCount()
        val entries = ArrayList<LogEntry>(count)
        repeat(times = count) { index ->
            val epochSeconds = data.readLong()
            val nanos = data.readInt()
            val pid = data.readInt()
            val tid = data.readInt()
            val letter = data.readChar()
            entries += LogEntry(
                id = firstId + index,
                epochSeconds = epochSeconds,
                nanos = nanos,
                pid = pid,
                tid = tid,
                level = LogLevel.entries.firstOrNull { it.letter == letter } ?: LogLevel.INFO,
                tag = data.readText(),
                message = data.readText(),
            )
        }
        return SavedLog(
            serial = serial,
            deviceName = deviceName,
            entries = entries,
            processNames = processNames,
        )
    }

    // writeUTF stops at 64 KB, and a message can be longer.
    private fun DataOutputStream.writeText(text: String) {
        val bytes = text.toByteArray(charset = Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readText(): String {
        val size = readCount()
        val bytes = ByteArray(size)
        readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun DataInputStream.readCount(): Int {
        val count = readInt()
        if (count < 0 || count > MAX_COUNT) throw IOException("Bad count $count")
        return count
    }

    private const val MAGIC = 0x424C4C47
    private const val VERSION = 1
    private const val MAX_COUNT = 64 * 1_024 * 1_024
}
