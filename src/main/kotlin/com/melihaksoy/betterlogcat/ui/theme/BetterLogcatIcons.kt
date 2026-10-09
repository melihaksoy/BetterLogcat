package com.melihaksoy.betterlogcat.ui.theme

import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icon.PathIconKey

/** Plugin icons for Jewel. Each has a `_dark` twin in resources. */
object BetterLogcatIcons {
    val Screenshot: IconKey = PathIconKey(
        path = "icons/screenshot.svg",
        iconClass = BetterLogcatIcons::class.java,
    )

    val ScreenRecord: IconKey = PathIconKey(
        path = "icons/screenRecord.svg",
        iconClass = BetterLogcatIcons::class.java,
    )
}
