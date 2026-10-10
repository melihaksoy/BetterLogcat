package com.melihaksoy.betterlogcat.ui.logcat.popup

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.ui.logcat.component.GutterUiModel
import java.awt.Point

/**
 * Native IDE popups the screen opens. Points are in Swing coordinates of the tool window panel.
 * The interface keeps composables free of Swing, so previews can pass [None].
 */
interface LogcatPopups {
    fun showDevicePicker(at: Point)
    fun showPackagePicker(at: Point)
    fun showRowMenu(
        entry: LogEntry,
        at: Point,
    )
    fun openSettings()
    fun openViewSettings()
    fun openInEditor(entry: LogEntry)
    fun showGutterMenu(
        entry: LogEntry,
        gutter: GutterUiModel,
        at: Point,
    )
    fun takeScreenshot()
    fun recordScreen()

    /** Right-click on a tag in the Tags panel: description and group actions. */
    fun showTagMenu(
        tag: String,
        at: Point,
    )

    /** Right-click on a tag group in the Tags panel. */
    fun showTagGroupMenu(
        groupName: String,
        at: Point,
    )

    object None : LogcatPopups {
        override fun showDevicePicker(at: Point) = Unit
        override fun showPackagePicker(at: Point) = Unit
        override fun showRowMenu(
            entry: LogEntry,
            at: Point,
        ) = Unit
        override fun openSettings() = Unit
        override fun openViewSettings() = Unit
        override fun openInEditor(entry: LogEntry) = Unit
        override fun showGutterMenu(
            entry: LogEntry,
            gutter: GutterUiModel,
            at: Point,
        ) = Unit
        override fun takeScreenshot() = Unit
        override fun recordScreen() = Unit
        override fun showTagMenu(
            tag: String,
            at: Point,
        ) = Unit
        override fun showTagGroupMenu(
            groupName: String,
            at: Point,
        ) = Unit
    }
}
