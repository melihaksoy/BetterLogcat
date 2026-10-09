package com.melihaksoy.betterlogcat.ui.logcat.popup

import com.melihaksoy.betterlogcat.model.LogEntry
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
    fun takeScreenshot()
    fun recordScreen()

    object None : LogcatPopups {
        override fun showDevicePicker(at: Point) = Unit
        override fun showPackagePicker(at: Point) = Unit
        override fun showRowMenu(
            entry: LogEntry,
            at: Point,
        ) = Unit
        override fun openSettings() = Unit
        override fun openViewSettings() = Unit
        override fun takeScreenshot() = Unit
        override fun recordScreen() = Unit
    }
}
