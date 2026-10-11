package com.melihaksoy.betterlogcat.ui

import com.melihaksoy.betterlogcat.model.TagGroup
import com.melihaksoy.betterlogcat.ui.logcat.model.TagNodeUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.TagSection
import com.melihaksoy.betterlogcat.ui.logcat.model.TagTree
import org.junit.Assert.assertEquals
import org.junit.Test

class TagTreeTest {

    private val counts = mapOf(
        "Adjust" to 249,
        "SnowplowTracker" to 20,
        "SnowplowEmitter" to 4,
        "Choreographer" to 30,
        "art" to 3,
    )
    private val snowplow = TagGroup(
        name = "Snowplow",
        tags = listOf("SnowplowEmitter", "SnowplowTracker", "SnowplowGone"),
    )

    @Test
    fun `groups sort in with the tags by their total count`() {
        val sections = TagTree.build(tagCounts = counts, groups = listOf(snowplow), appTags = emptySet(), query = "")

        assertEquals(1, sections.size)
        assertEquals(TagSection.ALL, sections.single().section)
        assertEquals(listOf("Adjust", "Choreographer", "Snowplow", "art"), sections.single().nodes.map { it.name })
        val group = sections.single().nodes[2] as TagNodeUiModel.Group
        assertEquals(24, group.count)
        // Busiest first, and tags not in the log stay out.
        assertEquals(listOf("SnowplowTracker", "SnowplowEmitter"), group.children.map { it.name })
    }

    @Test
    fun `a group with any app tag is in the app section`() {
        val sections = TagTree.build(tagCounts = counts, groups = listOf(snowplow), appTags = setOf("Adjust", "SnowplowEmitter"), query = "")

        assertEquals(listOf(TagSection.APP, TagSection.OTHER), sections.map { it.section })
        assertEquals(listOf("Adjust", "Snowplow"), sections[0].nodes.map { it.name })
        assertEquals(listOf("Adjust", "SnowplowTracker", "SnowplowEmitter"), sections[0].tags)
        assertEquals(listOf("Choreographer", "art"), sections[1].nodes.map { it.name })
    }

    @Test
    fun `a search keeps matching children, or all of them when the group name matches`() {
        val byTag = TagTree.build(tagCounts = counts, groups = listOf(snowplow), appTags = emptySet(), query = "emitter")
        val byName = TagTree.build(tagCounts = counts, groups = listOf(snowplow), appTags = emptySet(), query = "snowplow")

        assertEquals(listOf("SnowplowEmitter"), byTag.single().tags)
        assertEquals(listOf("SnowplowTracker", "SnowplowEmitter"), byName.single().tags)
    }

    @Test
    fun `a group with no tag in the log does not show`() {
        val sections = TagTree.build(tagCounts = mapOf("art" to 1), groups = listOf(snowplow), appTags = emptySet(), query = "")

        assertEquals(listOf("art"), sections.single().nodes.map { it.name })
    }
}
