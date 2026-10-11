package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable
import com.melihaksoy.betterlogcat.model.TagGroup

/** Sections of the Tags panel. [ALL] is the only one when no project app is known. */
enum class TagSection {
    ALL,
    APP,
    OTHER,
}

/** One node of the Tags panel tree: a tag, or a user group with its tags under it. */
@Immutable
sealed interface TagNodeUiModel {
    val name: String
    val count: Int

    @Immutable
    data class Tag(
        override val name: String,
        override val count: Int,
    ) : TagNodeUiModel

    /** [children] are the tags of the group that are in the log and match the search. */
    @Immutable
    data class Group(
        val group: TagGroup,
        val children: List<Tag>,
    ) : TagNodeUiModel {
        override val name: String
            get() = group.name
        override val count: Int = children.sumOf { it.count }
    }
}

/** A section and its nodes. Groups sort in with the tags, busiest first, like files and folders. */
@Immutable
data class TagSectionUiModel(
    val section: TagSection,
    val nodes: List<TagNodeUiModel>,
) {
    /** Every tag in the section, also the ones inside groups. */
    val tags: List<String> = nodes.flatMap { node ->
        when (node) {
            is TagNodeUiModel.Tag -> listOf(node.name)
            is TagNodeUiModel.Group -> node.children.map { it.name }
        }
    }
}

object TagTree {

    /**
     * Builds the Tags panel tree. A group shows only when one of its tags is in the log and matches
     * [query]; a query that matches the group name keeps all of its tags. A group is in the app section
     * when any of its tags is an app tag.
     */
    fun build(
        tagCounts: Map<String, Int>,
        groups: List<TagGroup>,
        appTags: Set<String>,
        query: String,
    ): List<TagSectionUiModel> {
        fun matches(name: String): Boolean = query.isEmpty() || name.contains(other = query, ignoreCase = true)
        val grouped = groups.flatMapTo(destination = HashSet()) { it.tags }
        val groupNodes = groups.mapNotNull { group ->
            val isNameMatch = query.isNotEmpty() && group.name.contains(other = query, ignoreCase = true)
            val children = group.tags
                .mapNotNull { tag ->
                    tagCounts[tag]?.let { count ->
                        TagNodeUiModel.Tag(
                            name = tag,
                            count = count,
                        )
                    }
                }
                .filter { isNameMatch || matches(name = it.name) }
                .sortedWith(comparator = BUSIEST_FIRST)
            if (children.isEmpty()) {
                null
            } else {
                TagNodeUiModel.Group(
                    group = group,
                    children = children,
                )
            }
        }
        val tagNodes = tagCounts
            .filter { (tag, _) -> tag !in grouped && matches(name = tag) }
            .map { (tag, count) ->
                TagNodeUiModel.Tag(
                    name = tag,
                    count = count,
                )
            }
        val nodes = (groupNodes + tagNodes).sortedWith(comparator = BUSIEST_FIRST)
        if (appTags.isEmpty()) {
            return listOf(
                TagSectionUiModel(
                    section = TagSection.ALL,
                    nodes = nodes,
                ),
            ).filter { it.nodes.isNotEmpty() }
        }
        val (appNodes, otherNodes) = nodes.partition { node ->
            when (node) {
                is TagNodeUiModel.Tag -> node.name in appTags
                is TagNodeUiModel.Group -> node.children.any { it.name in appTags }
            }
        }
        return listOf(
            TagSectionUiModel(
                section = TagSection.APP,
                nodes = appNodes,
            ),
            TagSectionUiModel(
                section = TagSection.OTHER,
                nodes = otherNodes,
            ),
        ).filter { it.nodes.isNotEmpty() }
    }

    private val BUSIEST_FIRST = compareByDescending<TagNodeUiModel> { it.count }.thenBy { it.name.lowercase() }
}
