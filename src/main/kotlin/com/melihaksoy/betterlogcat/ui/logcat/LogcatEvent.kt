package com.melihaksoy.betterlogcat.ui.logcat

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.ProblemKind

sealed interface LogcatEvent {
    data class ShowExportDialog(val defaultFileName: String) : LogcatEvent
    data class ShowStatus(val message: String) : LogcatEvent
    data object ShowMappingChooser : LogcatEvent

    /** A line that is not in the log any more, for example a saved problem. */
    data class OpenInEditor(val entry: LogEntry) : LogcatEvent

    /** A crash or ANR arrived. The screen notifies when the tool window is hidden. */
    data class AppCrashed(
        val entryId: Long,
        val kind: ProblemKind,
        val title: String,
    ) : LogcatEvent

    /** A native crash and its backtrace lines, to symbolize with the project's native libraries. */
    data class SymbolizeBacktrace(
        val crash: LogEntry,
        val backtrace: List<String>,
    ) : LogcatEvent
}
