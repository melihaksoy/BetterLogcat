package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable

/**
 * One problem of a session: every line of the same crash, exception, violation or warning (same
 * kind, tag and name, or the same warning text with other numbers in it). [sample] is the newest
 * line's message, kept so the problem can be read after its lines left the log.
 */
@Immutable
data class SavedProblem(
    val key: String,
    val kind: ProblemKind,
    /** The throwable's simple name, the signal, the violation, or the warning's first line. */
    val title: String,
    /** The text after the name, like the exception message or the ANR reason. Can be empty. */
    val detail: String,
    val tag: String,
    val count: Int,
    val firstEpochMillis: Long,
    val lastEpochMillis: Long,
    val level: LogLevel,
    val pid: Int,
    val tid: Int,
    val processName: String,
    val sample: String,
) {
    /** The newest line as a log entry, for the editor and for copy. Its id is not in any log. */
    fun toLogEntry(): LogEntry = LogEntry(
        id = NOT_IN_LOG,
        epochSeconds = Math.floorDiv(lastEpochMillis, 1_000L),
        nanos = Math.floorMod(lastEpochMillis, 1_000L).toInt() * 1_000_000,
        pid = pid,
        tid = tid,
        level = level,
        tag = tag,
        message = sample,
    )

    companion object {
        const val NOT_IN_LOG = -1L
    }
}

/**
 * The problems of one logcat session. A session starts when a pane starts to stream a device, and
 * again when the project app launches. Sessions are saved, so problems stay after Clear, a trimmed
 * log or an IDE restart, until the user deletes them.
 */
@Immutable
data class ProblemSession(
    val id: String,
    /** The device, and the app for a session that an app launch started. The user can rename it. */
    val title: String,
    /** The device the session streamed. A new session skips lines older than this device's saved ones. */
    val deviceSerial: String = "",
    val startedEpochMillis: Long,
    /** Worst kind first, then the most recent first. */
    val problems: List<SavedProblem>,
)

/** What one log line is as a problem: its group [key], [title] and [detail]. */
data class ProblemDescription(
    val key: String,
    val title: String,
    val detail: String,
)

object ProblemDescriptions {

    fun describe(
        entry: LogEntry,
        kind: ProblemKind,
    ): ProblemDescription {
        val firstLine = firstLine(message = entry.message)
        val name = entry.exceptionName
        val (what, title, detail) = when (kind) {
            ProblemKind.CRASH -> {
                val signal = if (LogProblems.isNativeCrash(tag = entry.tag, message = entry.message)) LogProblems.signalName(message = entry.message) else null
                when {
                    signal != null -> Triple(signal, signal, firstLine)
                    name != null -> Triple(name, simpleName(name = name), LogProblems.exceptionDetail(message = entry.message, name = name))
                    else -> Triple(normalized(text = firstLine), firstLine, "")
                }
            }
            ProblemKind.ANR -> {
                // "ANR in com.app (com.app/.MainActivity)", then "Reason: Input dispatching timed out …".
                val app = firstLine.removePrefix(prefix = "ANR in ").substringBefore(delimiter = ' ')
                val reason = entry.lines.firstOrNull { it.trimStart().startsWith(prefix = "Reason:") }?.substringAfter(delimiter = "Reason:")?.trim().orEmpty()
                Triple(app, firstLine, reason)
            }
            ProblemKind.STRICT_MODE -> {
                val violation = LogProblems.strictModeViolation(message = entry.message) ?: "StrictMode"
                Triple(violation, violation, DURATION.find(input = firstLine)?.value.orEmpty())
            }
            ProblemKind.EXCEPTION -> {
                val exception = name.orEmpty()
                Triple(exception, simpleName(name = exception), LogProblems.exceptionDetail(message = entry.message, name = exception))
            }
            // Warnings that differ only in numbers (ids, sizes, times) are one problem.
            ProblemKind.WARNING -> Triple(normalized(text = firstLine), firstLine, "")
        }
        return ProblemDescription(
            // U+241F splits the parts: it is not in tags or names, and XML can hold it, unlike NUL.
            key = "${kind.name}\u241F${entry.tag}\u241F$what",
            title = title.ifEmpty { firstLine },
            detail = detail,
        )
    }

    private fun firstLine(message: String): String = message.lineSequence().first().take(n = MAX_TITLE).trim()

    private fun simpleName(name: String): String = name.substringAfterLast(delimiter = '.')

    private fun normalized(text: String): String = text
        .replace(regex = HEX, replacement = "0x#")
        .replace(regex = DIGITS, replacement = "#")

    private const val MAX_TITLE = 300
    private val HEX = Regex(pattern = "0x[0-9a-fA-F]+")
    private val DIGITS = Regex(pattern = "\\d+")
    private val DURATION = Regex(pattern = """~duration=\d+ ms""")
}
