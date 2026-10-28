package com.melihaksoy.betterlogcat.toolwindow

import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.PackageFilter

/**
 * What a start of another project app does to the tabs, for example after a build variant switch. Every
 * `package:mine` pane changes to the app that ran before (see `LogcatIntent.PinProjectApp`), and [plan] says
 * which tab shows the new app. Both apps can run at once, so each tab names its app and none keeps `mine`.
 */
internal object AppSwitch {

    data class PaneInfo(
        val tabIndex: Int,
        val serial: String,
        val filter: LogFilter,
    )

    sealed interface Action {
        /** A tab for the new app is open already. */
        data class Focus(val tabIndex: Int) : Action

        /** A new tab: the filter of a `package:mine` pane, with the package of the new app. */
        data class Open(
            val serial: String,
            val query: String,
        ) : Action

        /** No pane follows the project app, so the switch changes nothing. */
        data object None : Action
    }

    /** Panes of [selectedTab] go first, so the new tab copies the filter the user looks at. */
    fun plan(
        panes: List<PaneInfo>,
        selectedTab: Int,
        newAppId: String,
    ): Action {
        val existing = panes.firstOrNull { it.filter.packageFilter == PackageFilter.Package(name = newAppId) }
        if (existing != null) return Action.Focus(tabIndex = existing.tabIndex)
        val source = panes
            .filter { it.filter.packageFilter == PackageFilter.ProjectApps }
            .minByOrNull { if (it.tabIndex == selectedTab) 0 else 1 }
            ?: return Action.None
        return Action.Open(
            serial = source.serial,
            query = source.filter.withPackageFilter(packageFilter = PackageFilter.Package(name = newAppId)).toQueryString(),
        )
    }
}
