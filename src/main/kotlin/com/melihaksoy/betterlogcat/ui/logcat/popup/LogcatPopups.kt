package com.melihaksoy.betterlogcat.ui.logcat.popup

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.ProblemKind
import com.melihaksoy.betterlogcat.model.ProblemSession
import com.melihaksoy.betterlogcat.model.SavedProblem
import com.melihaksoy.betterlogcat.network.NetworkCall
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

    /** Right-click on a problem in the Problems panel. */
    fun showProblemMenu(
        session: ProblemSession,
        problem: SavedProblem,
        at: Point,
    )

    /** Right-click on a session in the Problems panel: rename, delete. */
    fun showProblemSessionMenu(
        session: ProblemSession,
        at: Point,
    )

    /** The Problems panel's ⋮ menu: delete history by age, reset ignored problems. */
    fun showProblemsPanelMenu(at: Point)

    /** The export button: export, open a log file, compare with a log file. */
    fun showExportMenu(at: Point)

    /** Right-click on a call in the Network panel: copy its path, URL or cURL. */
    fun showNetworkCallMenu(
        call: NetworkCall,
        at: Point,
    )

    /** A crash or ANR arrived: notify when the tool window is hidden. */
    fun noticeCrash(
        entryId: Long,
        kind: ProblemKind,
        title: String,
    )

    /** Symbolizes a native crash's backtrace with the project's native libraries and opens the result. */
    fun symbolizeNativeCrash(
        crash: LogEntry,
        backtrace: List<String>,
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
        override fun showProblemMenu(
            session: ProblemSession,
            problem: SavedProblem,
            at: Point,
        ) = Unit
        override fun showProblemSessionMenu(
            session: ProblemSession,
            at: Point,
        ) = Unit
        override fun showProblemsPanelMenu(at: Point) = Unit
        override fun showExportMenu(at: Point) = Unit
        override fun showNetworkCallMenu(
            call: NetworkCall,
            at: Point,
        ) = Unit
        override fun noticeCrash(
            entryId: Long,
            kind: ProblemKind,
            title: String,
        ) = Unit
        override fun symbolizeNativeCrash(
            crash: LogEntry,
            backtrace: List<String>,
        ) = Unit
    }
}
