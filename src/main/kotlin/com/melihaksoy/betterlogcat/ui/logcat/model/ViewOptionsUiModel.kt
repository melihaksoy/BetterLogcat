package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable
import com.melihaksoy.betterlogcat.format.CopyMode
import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.TimeFormat

@Immutable
data class ViewOptionsUiModel(
    val softWrap: Boolean = true,
    val compactRows: Boolean = false,
    val expandByDefault: Boolean = true,
    val prettyJson: Boolean = true,
    val timeFormat: TimeFormat = TimeFormat.TIME,
    val sidePanel: SidePanel = SidePanel.NONE,
    val includeThreadId: Boolean = true,
    val showRepeatedTags: Boolean = true,
    val showRepeatedPackages: Boolean = true,
    val colorizeTags: Boolean = true,
    /** Cmd/Ctrl+C copies only messages, not the time, PID, package and tag. */
    val copyMessageOnly: Boolean = true,
    /** JSON and long messages have their own default: big responses stay one compact row until expanded. */
    val expandJsonByDefault: Boolean = false,
    val visibleColumns: Set<LogColumn> = LogColumn.entries.toSet(),
    val columnWidths: Map<LogColumn, Int> = LogColumn.entries.associateWith { it.defaultWidthDp },
) {
    fun width(column: LogColumn): Int = columnWidths[column] ?: column.defaultWidthDp

    fun isOn(toggle: ViewToggle): Boolean = when (toggle) {
        ViewToggle.SOFT_WRAP -> softWrap
        ViewToggle.COMPACT_ROWS -> compactRows
        ViewToggle.PRETTY_JSON -> prettyJson
        ViewToggle.INCLUDE_THREAD_ID -> includeThreadId
        ViewToggle.SHOW_REPEATED_TAGS -> showRepeatedTags
        ViewToggle.SHOW_REPEATED_PACKAGES -> showRepeatedPackages
        ViewToggle.COLORIZE_TAGS -> colorizeTags
        ViewToggle.COPY_MESSAGE_ONLY -> copyMessageOnly
        ViewToggle.EXPAND_JSON -> expandJsonByDefault
    }

    fun isExpandedByDefault(entry: LogEntry): Boolean =
        if (entry.json is JsonContent.Found || entry.isLong) expandJsonByDefault else expandByDefault

    val defaultCopyMode: CopyMode
        get() = if (copyMessageOnly) CopyMode.MESSAGE else CopyMode.FULL

    fun with(
        toggle: ViewToggle,
        isOn: Boolean,
    ): ViewOptionsUiModel = when (toggle) {
        ViewToggle.SOFT_WRAP -> copy(softWrap = isOn)
        ViewToggle.COMPACT_ROWS -> copy(compactRows = isOn)
        ViewToggle.PRETTY_JSON -> copy(prettyJson = isOn)
        ViewToggle.INCLUDE_THREAD_ID -> copy(includeThreadId = isOn)
        ViewToggle.SHOW_REPEATED_TAGS -> copy(showRepeatedTags = isOn)
        ViewToggle.SHOW_REPEATED_PACKAGES -> copy(showRepeatedPackages = isOn)
        ViewToggle.COLORIZE_TAGS -> copy(colorizeTags = isOn)
        ViewToggle.COPY_MESSAGE_ONLY -> copy(copyMessageOnly = isOn)
        ViewToggle.EXPAND_JSON -> copy(expandJsonByDefault = isOn)
    }
}
