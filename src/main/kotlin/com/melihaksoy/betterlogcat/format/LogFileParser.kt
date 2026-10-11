package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Entries read from a log file, and the process name of each pid the file names. */
data class ParsedLogFile(
    val entries: List<LogEntry>,
    val processNames: Map<Int, String>,
)

/**
 * Reads a file that Export wrote ([CopyMode.FULL]): `2026-10-11 22:24:14.352  1234-5678  com.app E Tag: message`,
 * with the lines of a multi-line message indented under the first. Lines that fit no entry are skipped.
 */
object LogFileParser {

    fun parse(
        text: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ParsedLogFile {
        val entries = ArrayList<LogEntry>()
        val processNames = HashMap<Int, String>()
        var header: Header? = null
        val message = StringBuilder()

        fun flush() {
            val current = header ?: return
            entries += LogEntry(
                id = entries.size + 1L,
                epochSeconds = current.epochSeconds,
                nanos = current.nanos,
                pid = current.pid,
                tid = current.tid,
                level = current.level,
                tag = current.tag,
                message = message.toString(),
            )
            message.setLength(0)
            header = null
        }

        text.lineSequence().forEach { line ->
            val match = HEADER.matchEntire(input = line)
            val parsed = match?.let {
                toHeader(
                    match = it,
                    zone = zone,
                )
            }
            if (parsed != null) {
                flush()
                header = parsed
                parsed.processName?.let { processNames[parsed.pid] = it }
                message.append(match.groupValues[MESSAGE_GROUP])
            } else if (header != null) {
                val indent = header?.messageColumn ?: 0
                val spaces = line.takeWhile { it == ' ' }.length
                message.append('\n').append(line.substring(startIndex = minOf(indent, spaces)))
            }
        }
        flush()
        return ParsedLogFile(
            entries = entries,
            processNames = processNames,
        )
    }

    private class Header(
        val epochSeconds: Long,
        val nanos: Int,
        val pid: Int,
        val tid: Int,
        val processName: String?,
        val level: LogLevel,
        val tag: String,
        val messageColumn: Int,
    )

    private fun toHeader(
        match: MatchResult,
        zone: ZoneId,
    ): Header? {
        val instant = try {
            LocalDateTime.parse(match.groupValues[1], TIME).atZone(zone).toInstant()
        } catch (_: DateTimeParseException) {
            return null
        }
        val level = LogLevel.entries.firstOrNull { it.letter.toString() == match.groupValues[5] } ?: return null
        return Header(
            epochSeconds = instant.epochSecond,
            nanos = instant.nano,
            pid = match.groupValues[2].toIntOrNull() ?: return null,
            tid = match.groupValues[3].toIntOrNull() ?: return null,
            processName = match.groups[4]?.value,
            level = level,
            tag = match.groupValues[6],
            messageColumn = match.groups[MESSAGE_GROUP]?.range?.first ?: 0,
        )
    }

    private const val MESSAGE_GROUP = 7
    private val TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val HEADER = Regex(pattern = """(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) +(\d+)-(\d+) +(?:(\S+) +)?([A-Z]) (.*?): (.*)""")
}
