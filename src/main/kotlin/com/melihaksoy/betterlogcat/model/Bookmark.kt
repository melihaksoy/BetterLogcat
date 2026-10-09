package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable

/**
 * Finds a log line again across sessions. Entry ids restart every session, so a bookmark keys on
 * what logcat itself reports: time, process, thread and tag.
 */
@Immutable
data class BookmarkKey(
    val epochSeconds: Long,
    val nanos: Int,
    val pid: Int,
    val tid: Int,
    val tag: String,
) {
    companion object {
        fun of(entry: LogEntry): BookmarkKey = BookmarkKey(
            epochSeconds = entry.epochSeconds,
            nanos = entry.nanos,
            pid = entry.pid,
            tid = entry.tid,
            tag = entry.tag,
        )
    }
}

/** A saved copy of a bookmarked line, so it stays readable after Clear, a trim or a restart. */
@Immutable
data class Bookmark(
    val key: BookmarkKey,
    val level: LogLevel,
    val message: String,
    val processName: String,
    val deviceName: String,
) {
    /** The saved line as an entry, for the editor and copy. [id] is not a live buffer id. */
    fun toLogEntry(id: Long): LogEntry = LogEntry(
        id = id,
        epochSeconds = key.epochSeconds,
        nanos = key.nanos,
        pid = key.pid,
        tid = key.tid,
        level = level,
        tag = key.tag,
        message = message,
    )

    companion object {
        fun of(
            entry: LogEntry,
            processName: String,
            deviceName: String,
        ): Bookmark = Bookmark(
            key = BookmarkKey.of(entry = entry),
            level = entry.level,
            message = entry.message,
            processName = processName,
            deviceName = deviceName,
        )
    }
}
