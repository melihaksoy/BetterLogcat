package com.melihaksoy.betterlogcat.ui.logcat

import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ui.logcat.component.PackageLabels

/** Tab name: device and package, for example "Pixel 9 · com.example.app". */
fun LogcatState.tabTitle(): String {
    val device = selectedDevice?.name ?: BetterLogcatBundle.message("device.none")
    val packageLabel = PackageLabels.label(
        filter = filter.packageFilter,
        projectAppIds = projectAppIds,
        currentAppId = currentAppId,
        runningPackages = packages,
        aliases = packageAliases,
    )
    val title = BetterLogcatBundle.message("tab.title", device, packageLabel)
    return if (crashCount > 0) BetterLogcatBundle.message("tab.title.crashes", title, crashCount) else title
}
