package com.melihaksoy.betterlogcat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagGroupTest {

    private val snowplow = TagGroup(
        name = "Snowplow",
        tags = listOf("SnowplowTracker", "SnowplowEmitter"),
    )

    @Test
    fun `encode and decode keep name, tags and their order`() {
        assertEquals(snowplow, TagGroup.decode(line = snowplow.encode()))
        assertEquals(TagGroup(name = "Spaced name", tags = listOf("Ktor Client")), TagGroup.decode(line = "Spaced name\tKtor Client"))
        assertNull(TagGroup.decode(line = "  \tart"))
    }

    @Test
    fun `adding a tag makes a new group or joins an existing one`() {
        val made = TagGroups.withTag(groups = emptyList(), tag = "art", groupName = " Noise ")
        assertEquals(listOf(TagGroup(name = "Noise", tags = listOf("art"))), made)

        val joined = TagGroups.withTag(groups = made, tag = "adbd", groupName = "Noise")
        assertEquals(listOf(TagGroup(name = "Noise", tags = listOf("art", "adbd"))), joined)
    }

    @Test
    fun `a tag is in one group at most, so adding moves it`() {
        val groups = listOf(snowplow, TagGroup(name = "Noise", tags = listOf("art")))

        val moved = TagGroups.withTag(groups = groups, tag = "SnowplowEmitter", groupName = "Noise")

        assertEquals(listOf("SnowplowTracker"), moved[0].tags)
        assertEquals(listOf("art", "SnowplowEmitter"), moved[1].tags)
    }

    @Test
    fun `removing the last tag keeps the empty group`() {
        val groups = TagGroups.withoutTag(groups = listOf(TagGroup(name = "Noise", tags = listOf("art"))), tag = "art")

        assertEquals(listOf(TagGroup(name = "Noise", tags = emptyList())), groups)
    }

    @Test
    fun `rename skips blank and taken names, delete keeps the others`() {
        val groups = listOf(snowplow, TagGroup(name = "Noise", tags = listOf("art")))

        assertEquals("Analytics", TagGroups.renamed(groups = groups, from = "Snowplow", to = " Analytics ").first().name)
        assertEquals(groups, TagGroups.renamed(groups = groups, from = "Snowplow", to = "Noise"))
        assertEquals(groups, TagGroups.renamed(groups = groups, from = "Snowplow", to = "  "))
        assertEquals(listOf("Noise"), TagGroups.deleted(groups = groups, name = "Snowplow").map { it.name })
        assertEquals("Snowplow", TagGroups.groupOf(groups = groups, tag = "SnowplowTracker")?.name)
    }
}
