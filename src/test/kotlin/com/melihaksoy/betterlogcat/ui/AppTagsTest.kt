package com.melihaksoy.betterlogcat.ui

import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.ui.logcat.appTags
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
}
