package com.melihaksoy.betterlogcat.ui

import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.ui.logcat.component.PackageLabels
import org.junit.Assert.assertEquals
import org.junit.Test

class PackageLabelsTest {

    @Test
    fun `the app launched last names project apps, not the first one alphabetically`() {
        val label = PackageLabels.label(
            filter = PackageFilter.ProjectApps,
            projectAppIds = setOf(
                OLD_VARIANT,
                NEW_VARIANT,
            ),
            currentAppId = NEW_VARIANT,
            runningPackages = listOf(
                OLD_VARIANT,
                NEW_VARIANT,
            ),
            aliases = emptyMap(),
        )

        assertEquals("$NEW_VARIANT +1", label)
    }

    @Test
    fun `an old variant that still runs names project apps when the current app is not running`() {
        val label = PackageLabels.label(
            filter = PackageFilter.ProjectApps,
            projectAppIds = setOf(
                OLD_VARIANT,
                NEW_VARIANT,
            ),
            currentAppId = NEW_VARIANT,
            runningPackages = listOf(OLD_VARIANT),
            aliases = emptyMap(),
        )

        assertEquals(OLD_VARIANT, label)
    }

    private companion object {
        const val OLD_VARIANT = "com.example.alpha.debug"
        const val NEW_VARIANT = "com.example.beta.debug"
    }
}
