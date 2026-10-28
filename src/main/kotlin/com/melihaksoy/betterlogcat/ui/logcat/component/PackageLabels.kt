package com.melihaksoy.betterlogcat.ui.logcat.component

import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.filter.PackageFilter

/** Text for the package picker, chooser rows and tab titles. Kept short: a project can have dozens of ids. */
internal object PackageLabels {

    /**
     * For "Project apps" this names the running project app. With more than one running, [currentAppId] goes
     * first, so the app launched last shows, not an old variant that still runs.
     */
    fun label(
        filter: PackageFilter,
        projectAppIds: Set<String>,
        currentAppId: String?,
        runningPackages: List<String>,
        aliases: Map<String, String>,
    ): String = when (filter) {
        PackageFilter.All -> BetterLogcatBundle.message("filter.package.all")
        PackageFilter.ProjectApps -> {
            val running = runningPackages.filter { it in projectAppIds }.sortedByDescending { it == currentAppId }
            when {
                projectAppIds.isEmpty() -> BetterLogcatBundle.message("filter.package.project.none")
                running.size == 1 -> aliases[running.single()] ?: running.single()
                running.size > 1 -> BetterLogcatBundle.message("filter.package.project.running", aliases[running.first()] ?: running.first(), running.size - 1)
                else -> BetterLogcatBundle.message("filter.package.project")
            }
        }
        is PackageFilter.Package -> aliases[filter.name] ?: filter.name
        is PackageFilter.Several -> BetterLogcatBundle.message("filter.package.several", filter.names.size)
    }

    /** Small grey text next to the picker label. */
    fun detail(filter: PackageFilter): String = when (filter) {
        PackageFilter.ProjectApps -> BetterLogcatBundle.message("filter.package.project.detail")
        PackageFilter.All, is PackageFilter.Package, is PackageFilter.Several -> ""
    }

    fun chooserLabel(
        filter: PackageFilter,
        projectAppIds: Set<String>,
        aliases: Map<String, String>,
    ): String = when (filter) {
        PackageFilter.ProjectApps -> BetterLogcatBundle.message("filter.package.project.chooser", projectAppIds.size)
        PackageFilter.All -> BetterLogcatBundle.message("filter.package.all")
        is PackageFilter.Package -> aliases[filter.name]?.let { BetterLogcatBundle.message("filter.package.aliased", it, filter.name) } ?: filter.name
        is PackageFilter.Several -> BetterLogcatBundle.message("filter.package.several", filter.names.size)
    }
}
