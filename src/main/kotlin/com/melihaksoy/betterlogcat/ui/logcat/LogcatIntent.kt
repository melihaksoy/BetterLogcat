package com.melihaksoy.betterlogcat.ui.logcat

import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.filter.QueryTerm
import com.melihaksoy.betterlogcat.format.CopyMode
import com.melihaksoy.betterlogcat.format.StackFrame
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewToggle
import java.io.File

sealed interface LogcatIntent {
    data class SelectDevice(val serial: String) : LogcatIntent

    data class ChangeQuery(val query: String) : LogcatIntent
    data object RefreshSavedFilters : LogcatIntent

    /** After a rendering crash: close side panels, so a saved panel cannot crash every start. */
    data object ResetViewAfterCrash : LogcatIntent
    data class SetLevelColor(
        val level: LogLevel,
        val colorArgb: Long?,
    ) : LogcatIntent
    data class AddHighlightRule(val rule: HighlightRule) : LogcatIntent
    data class RemoveHighlightRule(val rule: HighlightRule) : LogcatIntent
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
    data class HideTag(val tag: String) : LogcatIntent
    data class RemoveTagFilter(val tag: String) : LogcatIntent
    data class SetTagsVisible(
        val tags: List<String>,
        val isVisible: Boolean,
    ) : LogcatIntent
    data object ShowAllTags : LogcatIntent
    data class ToggleSidePanel(val panel: SidePanel) : LogcatIntent
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
}
