package com.melihaksoy.betterlogcat.ui

import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.ui.logcat.appTags
import com.melihaksoy.betterlogcat.ui.logcat.packageTagCounts
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TreeSet

class AppTagsTest {

    @Test
    fun `tags from unknown processes do not crash and are not app tags`() {
        val state = LogcatState(
            // A sorted set throws on a null lookup; the tag of pid 99 has no process name yet.
            projectAppIds = TreeSet(listOf("com.example.app")),
            processNames = mapOf(10 to "com.example.app:sync"),
            tagPids = mapOf(
                "OkHttp" to setOf(10),
                "Mystery" to setOf(99),
            ),
        )

        assertEquals(setOf("OkHttp"), appTags(state = state))
    }

    @Test
    fun `package mine keeps only tags the app logged, shared tags too`() {
        val state = sharedTagState(query = "package:mine ")

        assertEquals(
            mapOf(
                "OkHttp" to 4,
                "HWUI" to 9,
            ),
            packageTagCounts(state = state),
        )
    }

    @Test
    fun `an excluded package drops only its own tags`() {
        val state = sharedTagState(query = "-package:com.android.systemui ")

        assertEquals(
            mapOf(
                "OkHttp" to 4,
                "HWUI" to 9,
                "Mystery" to 1,
            ),
            packageTagCounts(state = state),
        )
    }

    @Test
    fun `without a package term every tag stays`() {
        val state = sharedTagState(query = "tag:Ok ")

        assertEquals(state.tagCounts, packageTagCounts(state = state))
    }

    private fun sharedTagState(query: String) = LogcatState(
        filter = LogFilter().withQueryString(text = query),
        projectAppIds = setOf("com.example.app"),
        processNames = mapOf(
            10 to "com.example.app",
            20 to "com.android.systemui",
        ),
        tagCounts = mapOf(
            "OkHttp" to 4,
            "HWUI" to 9,
            "SystemUi" to 7,
            "Mystery" to 1,
        ),
        tagPids = mapOf(
            "OkHttp" to setOf(10),
            "HWUI" to setOf(20, 10),
            "SystemUi" to setOf(20),
            // No process name yet: hidden by package:mine, kept by an exclude.
            "Mystery" to setOf(99),
        ),
    )
}
