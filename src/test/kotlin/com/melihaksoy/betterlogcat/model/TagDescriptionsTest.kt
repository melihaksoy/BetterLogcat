package com.melihaksoy.betterlogcat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagDescriptionsTest {

    @Test
    fun `built-in descriptions load from the resource file`() {
        val builtIn = TagDescriptions.builtIn

        assertTrue(builtIn.size.toString(), builtIn.size >= 50)
        assertTrue(builtIn.getValue("Choreographer").contains("Skipped"))
        assertTrue(builtIn.getValue("System.out").isNotBlank())
        assertTrue(builtIn.values.all { it.isNotBlank() && '\n' !in it })
    }

    @Test
    fun `the user's note wins, and a blank note falls back to the built-in one`() {
        assertEquals("Our analytics", TagDescriptions.describe(tag = "Choreographer", userNotes = mapOf("Choreographer" to "Our analytics")))
        assertEquals(TagDescriptions.builtIn["Choreographer"], TagDescriptions.describe(tag = "Choreographer", userNotes = mapOf("Choreographer" to " ")))
        assertEquals("Tracker events", TagDescriptions.describe(tag = "SnowplowTracker", userNotes = mapOf("SnowplowTracker" to "Tracker events")))
        assertNull(TagDescriptions.describe(tag = "SnowplowTracker", userNotes = emptyMap()))
    }
}
