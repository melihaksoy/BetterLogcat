package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagVisibilityTest {

    private val known = listOf("art", "adbd", "OkHttp", "Garage")

    @Test
    fun `hiding tags writes one exclude term into the search text`() {
        val filter = TagVisibility.setVisible(
            filter = LogFilter().withQueryString(text = "package:mine timeout"),
            tags = listOf("art", "adbd"),
            isVisible = false,
            knownTags = known,
        )

        assertEquals("package:mine -tag=:art,adbd timeout ", filter.query)
        assertFalse(filter.isTagVisible(tag = "art"))
        assertFalse(filter.isTagVisible(tag = "adbd"))
        assertTrue(filter.isTagVisible(tag = "OkHttp"))
    }

    @Test
    fun `showing a hidden tag takes it out of the term`() {
        val filter = TagVisibility.setVisible(
            filter = LogFilter().withQueryString(text = "-tag=:art,adbd"),
            tags = listOf("art"),
            isVisible = true,
            knownTags = known,
        )

        assertEquals("-tag=:adbd ", filter.query)
    }

    @Test
    fun `showing the last hidden tag removes the term`() {
        val filter = TagVisibility.setVisible(
            filter = LogFilter().withQueryString(text = "package:mine -tag=:art"),
            tags = listOf("art"),
            isVisible = true,
            knownTags = known,
        )

        assertEquals("package:mine ", filter.query)
    }

    @Test
    fun `show only then add another tag`() {
        val only = TagVisibility.showOnly(
            filter = LogFilter().withQueryString(text = "package:mine -tag=:art"),
            tag = "OkHttp",
        )
        val plusGarage = TagVisibility.setVisible(
            filter = only,
            tags = listOf("Garage"),
            isVisible = true,
            knownTags = known,
        )

        assertEquals("package:mine tag=:OkHttp ", only.query)
        assertEquals("package:mine tag=:OkHttp,Garage ", plusGarage.query)
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

    @Test
    fun `a long hide list turns into a short show list`() {
        val many = (1..20).map { "Tag$it" }
        val hidden = TagVisibility.setVisible(
            filter = LogFilter(),
            tags = many,
            isVisible = false,
            knownTags = many,
        )
        val one = TagVisibility.setVisible(
            filter = hidden,
            tags = listOf("Tag7"),
            isVisible = true,
            knownTags = many,
        )

        assertEquals("tag=:Tag7 ", one.query)
    }

    @Test
    fun `tags with spaces stay one term`() {
        val filter = TagVisibility.setVisible(
            filter = LogFilter(),
            tags = listOf("Ktor Client", "art"),
            isVisible = false,
            knownTags = known,
        )

        assertEquals("-tag=:\"Ktor Client,art\" ", filter.query)
        assertFalse(filter.isTagVisible(tag = "Ktor Client"))
        assertTrue(filter.isTagVisible(tag = "Ktor"))
    }

    @Test
    fun `show all clears typed tag terms too`() {
        val filter = TagVisibility.showAll(filter = LogFilter().withQueryString(text = "package:mine tag:Ok -tag=:art crash"))

        assertEquals("package:mine crash ", filter.query)
    }

    @Test
    fun `app start and end lines stay with a show list but not with a hide list`() {
        val start = LogEntry(
            id = 1,
            epochSeconds = 1_791_540_000L,
            nanos = 0,
            pid = 1_822,
            tid = 1_822,
            level = LogLevel.INFO,
            tag = "ActivityManager",
            message = "Start proc 15011:com.example.app/u0a215 for next-top-activity {com.example.app/.MainActivity}",
        )
        val other = start.withMessage(message = "Background start not allowed")

        fun matcher(query: String) = LogFilterMatcher(
            filter = LogFilter().withQueryString(text = query),
            processNames = mapOf(1_822 to "system_server"),
            projectAppIds = setOf("com.example.app"),
        )

        assertTrue(matcher(query = "package:mine tag=:OkHttp,Garage").matches(entry = start))
        assertFalse(matcher(query = "tag=:OkHttp,Garage").matches(entry = other))
        assertFalse(matcher(query = "package:mine -tag=:ActivityManager").matches(entry = start))
    }

    private fun LogFilter.isTagVisible(tag: String): Boolean = LogFilterMatcher(
        filter = this,
        processNames = emptyMap(),
    ).isTagVisible(tag = tag)

    @Test
    fun `show only a group writes one show term with all its tags`() {
        val filter = TagVisibility.showOnly(
            filter = LogFilter().withQueryString(text = "package:mine -tag=:art"),
            tags = listOf("SnowplowTracker", "SnowplowEmitter", "SnowplowTracker"),
        )

        assertTrue(filter.isTagVisible(tag = "SnowplowTracker"))
        assertTrue(filter.isTagVisible(tag = "SnowplowEmitter"))
        assertFalse(filter.isTagVisible(tag = "art"))
        assertFalse(filter.isTagVisible(tag = "OkHttp"))
        assertEquals(filter, TagVisibility.showOnly(filter = filter, tags = emptyList()))
    }
}
