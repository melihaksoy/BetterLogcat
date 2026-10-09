package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry

enum class CopyMode {
    /** Header columns plus message. Continuation lines line up under the first message line. */
    FULL,

    /** Message text only, no header. */
    MESSAGE,

    /** JSON part of each message, pretty printed. Entries without JSON fall back to the message. */
    JSON_PRETTY,

    /** JSON part of each message, on one line. Entries without JSON fall back to the message. */
    JSON_MINIFIED,
}

object LogCopyFormatter {

    fun format(
        entries: List<LogEntry>,
        mode: CopyMode,
        processNames: Map<Int, String>,
    ): String = when (mode) {
        CopyMode.FULL -> entries.joinToString(separator = "\n") { entry ->
            fullLine(
                entry = entry,
                processNames = processNames,
            )
        }
        CopyMode.MESSAGE -> entries.joinToString(separator = "\n") { it.message }
        CopyMode.JSON_PRETTY -> entries.joinToString(separator = "\n\n") { entry ->
            when (val json = entry.json) {
                is JsonContent.Found -> if (json.main.isComplete) json.pretty else json.raw
                JsonContent.None -> entry.message
            }
        }
        CopyMode.JSON_MINIFIED -> entries.joinToString(separator = "\n") { entry ->
            when (val json = entry.json) {
                is JsonContent.Found -> json.minified
                JsonContent.None -> entry.message
            }
        }
    }

    private fun fullLine(
        entry: LogEntry,
        processNames: Map<Int, String>,
    ): String {
        val header = buildString {
            append(
                LogTimeFormatter.dateTime(
                    epochSeconds = entry.epochSeconds,
                    nanos = entry.nanos,
                ),
            )
            append(' ')
            append(entry.pid.toString().padStart(length = 5))
            append('-')
            append(entry.tid.toString().padEnd(length = 5))
            append(' ')
            processNames[entry.pid]?.let {
                append(it)
                append(' ')
            }
            append(entry.level.letter)
            append(' ')
            append(entry.tag)
            append(": ")
        }
        val indent = " ".repeat(n = header.length)
        return entry.lines.mapIndexed { index, line -> if (index == 0) header + line else indent + line }
            .joinToString(separator = "\n")
    }
}
