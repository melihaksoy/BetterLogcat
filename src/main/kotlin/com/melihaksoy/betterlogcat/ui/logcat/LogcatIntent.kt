package com.melihaksoy.betterlogcat.ui.logcat

import com.melihaksoy.betterlogcat.model.BookmarkKey
import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.filter.QueryTerm
import com.melihaksoy.betterlogcat.format.CopyMode
import com.melihaksoy.betterlogcat.format.StackFrame
import com.melihaksoy.betterlogcat.model.HighlightPreset
import com.melihaksoy.betterlogcat.model.HighlightRole
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewToggle
import java.io.File

sealed interface LogcatIntent {
    data class SelectDevice(val serial: String) : LogcatIntent

    data class ChangeQuery(val query: String) : LogcatIntent
    data object RefreshSavedFilters : LogcatIntent

    data class ToggleBookmark(val id: Long) : LogcatIntent
    data class JumpToBookmark(val delta: Int) : LogcatIntent
    data class RemoveBookmark(val key: BookmarkKey) : LogcatIntent
    data object JumpToCrash : LogcatIntent

    /** Copies one entry, whatever the selection is. */
    data class CopyEntry(
        val id: Long,
        val mode: CopyMode,
    ) : LogcatIntent

    /** Selects and shows an entry by id, for example from the network panel. */
    data class JumpToEntry(val id: Long) : LogcatIntent

    /** After a rendering crash: close side panels, so a saved panel cannot crash every start. */
    data object ResetViewAfterCrash : LogcatIntent
    data class SetLevelColor(
        val level: LogLevel,
        val colorArgb: Long?,
    ) : LogcatIntent
    data class AddHighlightRule(val rule: HighlightRule) : LogcatIntent
    data class RemoveHighlightRule(val rule: HighlightRule) : LogcatIntent
    data class AddHiddenUrlBase(val base: String) : LogcatIntent
    data class RemoveHiddenUrlBase(val base: String) : LogcatIntent
    data class SetPresetEnabled(
        val preset: HighlightPreset,
        val isEnabled: Boolean,
    ) : LogcatIntent
    data class SetPresetRoleEnabled(
        val preset: HighlightPreset,
        val role: HighlightRole,
        val isEnabled: Boolean,
    ) : LogcatIntent

    /** A null [colorArgb] goes back to the role's default. */
    data class SetHighlightRoleColor(
        val role: HighlightRole,
        val colorArgb: Long?,
    ) : LogcatIntent
    data class SetPackageAlias(
        val packageName: String,
        val alias: String,
    ) : LogcatIntent
    data class ApplySavedFilter(val query: String) : LogcatIntent
    data class ToggleFavoriteFilter(val query: String) : LogcatIntent
    data class RemoveFromHistory(val query: String) : LogcatIntent
    data class SetRegex(val isRegex: Boolean) : LogcatIntent
    data class SetMatchCase(val isMatchCase: Boolean) : LogcatIntent
    data class ToggleLevel(val level: LogLevel) : LogcatIntent
    data class SelectPackage(val packageFilter: PackageFilter) : LogcatIntent
    data class ShowOnlyTag(val tag: String) : LogcatIntent
    data class ShowOnlyTags(val tags: List<String>) : LogcatIntent
    data class HideTag(val tag: String) : LogcatIntent
    data class SetTagsVisible(
        val tags: List<String>,
        val isVisible: Boolean,
    ) : LogcatIntent
    data object ShowAllTags : LogcatIntent

    /** A blank [note] removes the user's note, so the built-in description shows again. */
    data class SetTagNote(
        val tag: String,
        val note: String,
    ) : LogcatIntent
    data class AddTagToGroup(
        val tag: String,
        val groupName: String,
    ) : LogcatIntent
    /** Tags dragged together in the Tags panel: all go in the group [groupName], new or not. */
    data class GroupTags(
        val tags: List<String>,
        val groupName: String,
    ) : LogcatIntent
    data class UngroupTags(val tags: List<String>) : LogcatIntent
    data class RenameTagGroup(
        val from: String,
        val to: String,
    ) : LogcatIntent
    data class DeleteTagGroup(val name: String) : LogcatIntent
    data class ToggleSidePanel(val panel: SidePanel) : LogcatIntent
    data class ResizeSidePanel(
        val panel: SidePanel,
        val widthDp: Int,
    ) : LogcatIntent
    data class ResizeNetworkDetail(val heightDp: Int) : LogcatIntent

    /** Opens a call in the network details pane; null closes it. */
    data class SelectNetworkCall(val call: NetworkCall?) : LogcatIntent
    data object ResetFilters : LogcatIntent

    data object TogglePause : LogcatIntent
    data object Clear : LogcatIntent

    data class SetViewToggle(
        val toggle: ViewToggle,
        val isOn: Boolean,
    ) : LogcatIntent
    data class SetTimeFormat(val format: TimeFormat) : LogcatIntent
    data class ToggleColumn(val column: LogColumn) : LogcatIntent
    data class ResizeColumn(
        val column: LogColumn,
        val widthDp: Int,
    ) : LogcatIntent

    data class ToggleExpanded(val id: Long) : LogcatIntent
    data class SetSelectionExpanded(val isExpanded: Boolean) : LogcatIntent
    data object ExpandAll : LogcatIntent
    data object CollapseAll : LogcatIntent

    data class PressRow(
        val id: Long,
        val isExtend: Boolean,
        val isToggle: Boolean,
    ) : LogcatIntent

    data class DragSelect(
        val anchorId: Long,
        val currentId: Long,
    ) : LogcatIntent

    data class MoveSelection(
        val delta: Int,
        val isExtend: Boolean,
    ) : LogcatIntent

    data object SelectAll : LogcatIntent
    data object ClearSelection : LogcatIntent

    data class Copy(val mode: CopyMode) : LogcatIntent
    data object Export : LogcatIntent
    data class ExportTo(val file: File) : LogcatIntent
    data class OpenFrame(val frame: StackFrame) : LogcatIntent
    data object ChooseMapping : LogcatIntent
    data class LoadMapping(val file: File) : LogcatIntent
    data object RemoveMapping : LogcatIntent

    /** Goes to a line of a problem: [back] 0 is the newest line, 1 the one before, and so on. */
    data class JumpToProblem(
        val sessionId: String,
        val key: String,
        val back: Int,
    ) : LogcatIntent
    data class DeleteProblem(
        val sessionId: String,
        val key: String,
    ) : LogcatIntent
    data class DeleteProblemSession(val id: String) : LogcatIntent
    data class RenameProblemSession(
        val id: String,
        val title: String,
    ) : LogcatIntent

    /** Deletes saved sessions that started before [epochMillis]. The live session stays. */
    data class DeleteProblemHistoryBefore(val epochMillis: Long) : LogcatIntent
    data class IgnoreProblem(val key: String) : LogcatIntent
    data object ResetIgnoredProblems : LogcatIntent

    /** Shows a saved log file in this pane instead of the device stream. */
    data class OpenLogFile(val file: File) : LogcatIntent
    data object CloseLogFile : LogcatIntent

    /** Collects the native backtrace after a "Fatal signal" line, for symbolizing. */
    data class SymbolizeNativeCrash(val id: Long) : LogcatIntent
}
