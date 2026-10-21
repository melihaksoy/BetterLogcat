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
    val tagsPanelWidth: Int = 280,
    val networkPanelWidth: Int = 460,
    val bookmarksPanelWidth: Int = 360,
    val problemsPanelWidth: Int = 380,
    /** Height of the request/response details in the network panel. */
    val networkDetailHeight: Int = 320,
    val includeThreadId: Boolean = true,
    val showRepeatedTags: Boolean = true,
    val showRepeatedPackages: Boolean = true,
    val colorizeTags: Boolean = true,
    /** Cmd/Ctrl+C copies only messages, not the time, PID, package and tag. */
    val copyMessageOnly: Boolean = true,
    /** JSON and long messages have their own default: big responses stay one compact row until expanded. */
    val expandJsonByDefault: Boolean = false,
    /** When the project app starts, the log starts over with it. */
    val clearOnAppLaunch: Boolean = false,
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
        ViewToggle.CLEAR_ON_APP_LAUNCH -> clearOnAppLaunch
    }

    fun isExpandedByDefault(entry: LogEntry): Boolean =
        if (entry.json is JsonContent.Found || entry.isLong) expandJsonByDefault else expandByDefault

    /**
     * Expand shows more of [entry]. Without soft wrap, one long line without JSON has nothing more to show:
     * it shows whole and scrolls sideways, with no expand toggle.
     */
    fun isExpandable(entry: LogEntry): Boolean =
        entry.isExpandable && (softWrap || entry.lines.size > 1 || entry.json is JsonContent.Found)

    /** [isToggled]: the user flipped this entry away from its default. */
    fun isExpanded(
        entry: LogEntry,
        isToggled: Boolean,
    ): Boolean = when {
        !entry.isExpandable -> false
        !isExpandable(entry = entry) -> true
        else -> isExpandedByDefault(entry = entry) xor isToggled
    }

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
        ViewToggle.CLEAR_ON_APP_LAUNCH -> copy(clearOnAppLaunch = isOn)
    }
}
