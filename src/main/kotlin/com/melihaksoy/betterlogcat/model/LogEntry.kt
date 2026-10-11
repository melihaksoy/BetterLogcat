package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable
import com.melihaksoy.betterlogcat.format.JsonFormatter
import com.melihaksoy.betterlogcat.format.StackFrameParser

/**
 * One logcat message. A multi-line message (stack trace, JSON) stays one entry.
 * Derived values are lazy because most entries never render.
 */
@Immutable
class LogEntry(
    val id: Long,
    val epochSeconds: Long,
    val nanos: Int,
    val pid: Int,
    val tid: Int,
    val level: LogLevel,
    val tag: String,
    val message: String,
) {
    val lines: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) { message.split('\n') }

    val json: JsonContent by lazy(LazyThreadSafetyMode.PUBLICATION) { JsonFormatter.detect(message = message) }

    val hasStackFrames: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        StackFrameParser.containsFrame(text = message)
    }

    /** An app crash: an assert, AndroidRuntime's FATAL EXCEPTION, or libc's native "Fatal signal". */
    val isCrash: Boolean
        get() = level == LogLevel.ASSERT ||
            (level == LogLevel.ERROR && tag == "AndroidRuntime" && message.startsWith("FATAL EXCEPTION")) ||
            LogProblems.isNativeCrash(tag = tag, message = message)

    /** The first throwable class the message names, or null. See [LogProblems.exceptionName]. */
    val exceptionName: String? by lazy(LazyThreadSafetyMode.PUBLICATION) { LogProblems.exceptionName(message = message) }

    /** Crash, exception or warning, for the gutter and the Problems panel. Null for other lines. */
    val problem: ProblemKind? by lazy(LazyThreadSafetyMode.PUBLICATION) { LogProblems.kindOf(entry = this) }

    /** App process start or end, when this is one of ActivityManager's lines about it. Null otherwise. */
    val processEvent: ProcessEvent? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ProcessEvent.parse(
            tag = tag,
            message = message,
        )
    }

    /** Long enough to fill several rows. Long messages collapse like JSON. */
    val isLong: Boolean
        get() = message.length > LONG_MESSAGE_CHARS

    val isExpandable: Boolean
        get() = lines.size > 1 || isLong || json is JsonContent.Found

    /** Same entry with a new message, for example after retrace. Keeps the id, so selection survives. */
    fun withMessage(message: String): LogEntry = LogEntry(
        id = id,
        epochSeconds = epochSeconds,
        nanos = nanos,
        pid = pid,
        tid = tid,
        level = level,
        tag = tag,
        message = message,
    )

    /** Same id and message: a merged or retraced entry keeps its id but must still redraw. */
    override fun equals(other: Any?): Boolean = other is LogEntry && other.id == id && other.message == message

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val LONG_MESSAGE_CHARS = 400
    }
}
