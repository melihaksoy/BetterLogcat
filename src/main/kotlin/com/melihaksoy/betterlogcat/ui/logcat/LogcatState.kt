package com.melihaksoy.betterlogcat.ui.logcat

import androidx.compose.runtime.Immutable
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.ui.logcat.model.BookmarkUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.LogcatContentUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.NetworkDetailUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SavedFiltersUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SelectionUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewOptionsUiModel

/**
 * The toolbar shows in every state, so the screen state is one class. The distinct
 * states of the main area live in [LogcatContentUiModel].
 */
@Immutable
data class LogcatState(
    val content: LogcatContentUiModel = LogcatContentUiModel.Loading,
    val devices: List<Device> = emptyList(),
    val selectedSerial: String = "",
    val entries: List<LogEntry> = emptyList(),
    val totalCount: Int = 0,
    val filter: LogFilter = LogFilter(),
    val isQueryValid: Boolean = true,
    val processNames: Map<Int, String> = emptyMap(),
    val packages: List<String> = emptyList(),
    val projectAppIds: Set<String> = emptySet(),
    /** Package name to short display name. Visual only. */
    val packageAliases: Map<String, String> = emptyMap(),
    /** User colors that replace the theme color of a level. */
    val levelColors: Map<LogLevel, Long> = emptyMap(),
    val highlightRules: List<HighlightRule> = emptyList(),
    val tagCounts: Map<String, Int> = emptyMap(),
    /** Process ids seen per tag. */
    val tagPids: Map<String, Set<Int>> = emptyMap(),
    val viewOptions: ViewOptionsUiModel = ViewOptionsUiModel(),
    val selection: SelectionUiModel = SelectionUiModel(),
    val toggledIds: Set<Long> = emptySet(),
    val savedFilters: SavedFiltersUiModel = SavedFiltersUiModel(),
    val networkCalls: List<NetworkCall> = emptyList(),
    /** The call open in the network details pane; null when none is open. */
    val networkDetail: NetworkDetailUiModel? = null,
    val crashCount: Int = 0,
    /** Ids of bookmarked lines that are in this pane's log. */
    val bookmarkedIds: Set<Long> = emptySet(),
    /** Every bookmark of the project, oldest first, also lines that left the log. */
    val bookmarks: List<BookmarkUiModel> = emptyList(),
    /** Entries per second per tag over the last 30 seconds, oldest first. */
    val tagRates: Map<String, IntArray> = emptyMap(),
    val isPaused: Boolean = false,
    /** Name of the loaded R8/ProGuard mapping file. Empty when none. */
    val mappingFileName: String = "",
    val pausedNewCount: Int = 0,
) {
    val selectedDevice: Device?
        get() = devices.firstOrNull { it.serial == selectedSerial }

    fun isExpanded(entry: LogEntry): Boolean = entry.isExpandable && (viewOptions.isExpandedByDefault(entry = entry) xor (entry.id in toggledIds))
}
