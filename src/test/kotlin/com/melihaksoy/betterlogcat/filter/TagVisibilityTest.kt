package com.melihaksoy.betterlogcat.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagVisibilityTest {

    private val known = listOf("art", "adbd", "OkHttp", "Garage")

    @Test
    fun `hiding tags adds them to the exclude list`() {
        val filter = TagVisibility.setVisible(
            filter = LogFilter(),
            tags = listOf("art", "adbd"),
            isVisible = false,
            knownTags = known,
        )

        assertFalse(filter.isTagVisible(tag = "art"))
        assertFalse(filter.isTagVisible(tag = "adbd"))
        assertTrue(filter.isTagVisible(tag = "OkHttp"))
    }

    @Test
    fun `showing a hidden tag removes it from the exclude list`() {
        val filter = TagVisibility.setVisible(
            filter = LogFilter(excludedTags = listOf("art", "adbd")),
            tags = listOf("art"),
            isVisible = true,
            knownTags = known,
        )

        assertEquals(listOf("adbd"), filter.excludedTags)
    }

    @Test
    fun `show only then add another tag`() {
        val only = TagVisibility.showOnly(
            filter = LogFilter(),
            tag = "OkHttp",
        )
        val plusGarage = TagVisibility.setVisible(
            filter = only,
            tags = listOf("Garage"),
            isVisible = true,
            knownTags = known,
        )

        assertTrue(plusGarage.isTagVisible(tag = "OkHttp"))
        assertTrue(plusGarage.isTagVisible(tag = "Garage"))
        assertFalse(plusGarage.isTagVisible(tag = "art"))
    }

    @Test
    fun `hiding the last shown tag does not bring every tag back`() {
        val only = TagVisibility.showOnly(
            filter = LogFilter(),
            tag = "OkHttp",
        )
        val none = TagVisibility.setVisible(
            filter = only,
            tags = listOf("OkHttp"),
            isVisible = false,
            knownTags = known,
        )

        assertTrue(known.none { none.isTagVisible(tag = it) })
    }
}
