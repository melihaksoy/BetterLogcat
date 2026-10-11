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
    fun `removing the last tag removes the group`() {
        val groups = TagGroups.withoutTag(groups = listOf(TagGroup(name = "Noise", tags = listOf("art"))), tag = "art")

        assertEquals(emptyList<TagGroup>(), groups)
    }

    @Test
    fun `moving the last tag out removes its old group`() {
        val groups = listOf(TagGroup(name = "Noise", tags = listOf("art")), TagGroup(name = "Ads", tags = listOf("Adjust")))

        val moved = TagGroups.withTag(groups = groups, tag = "art", groupName = "Ads")

        assertEquals(listOf(TagGroup(name = "Ads", tags = listOf("Adjust", "art"))), moved)
    }

    @Test
    fun `grouping takes the tags out of their groups into a new one`() {
        val groups = TagGroups.grouped(groups = listOf(snowplow), tags = listOf("art", "SnowplowEmitter"), groupName = "Group 1")

        assertEquals(
            listOf(TagGroup(name = "Snowplow", tags = listOf("SnowplowTracker")), TagGroup(name = "Group 1", tags = listOf("art", "SnowplowEmitter"))),
            groups,
        )
    }

    @Test
    fun `ungrouping takes each tag out and drops groups left empty`() {
        val groups = listOf(snowplow, TagGroup(name = "Noise", tags = listOf("art")))

        val ungrouped = TagGroups.ungrouped(groups = groups, tags = listOf("art", "SnowplowEmitter", "Adjust"))

        assertEquals(listOf(TagGroup(name = "Snowplow", tags = listOf("SnowplowTracker"))), ungrouped)
    }

    @Test
    fun `next name skips taken default names`() {
        val groups = listOf(TagGroup(name = "Group 1", tags = listOf("art")), TagGroup(name = "Group 3", tags = listOf("adbd")))

        assertEquals("Group 2", TagGroups.nextName(groups = groups) { "Group $it" })
        assertEquals("Group 1", TagGroups.nextName(groups = emptyList()) { "Group $it" })
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
