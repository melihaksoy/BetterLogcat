package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable

/**
 * A folder of tags in the Tags panel that the user made, for example every tag of one library.
 * Grouping is manual only: a tag is in a group because the user put it there. A tag is in one
 * group at most.
 */
@Immutable
data class TagGroup(
    val name: String,
    val tags: List<String>,
) {
    /** One line for the settings file: the name, then each tag, split by tabs. Tags have no tabs. */
    fun encode(): String = (listOf(name) + tags).joinToString(separator = "\t")

    companion object {
        fun decode(line: String): TagGroup? {
            val parts = line.split('\t')
            val name = parts.first().trim()
            if (name.isEmpty()) return null
            return TagGroup(
                name = name,
                tags = parts.drop(1).filter { it.isNotEmpty() }.distinct(),
            )
        }
    }
}

/** Changes to the list of groups. Each returns a new list and keeps the order of the groups. */
object TagGroups {

    /** Puts [tag] in the group [groupName], out of any other group. Makes the group when it is new. */
    fun withTag(
        groups: List<TagGroup>,
        tag: String,
        groupName: String,
    ): List<TagGroup> {
        val name = groupName.trim()
        if (name.isEmpty()) return groups
        val without = withoutTag(
            groups = groups,
            tag = tag,
        )
        return if (without.any { it.name == name }) {
            without.map { if (it.name == name) it.copy(tags = it.tags + tag) else it }
        } else {
            without + TagGroup(
                name = name,
                tags = listOf(tag),
            )
        }
    }

    /** Puts [tags] together in a new group [groupName], out of any other group. */
    fun grouped(
        groups: List<TagGroup>,
        tags: List<String>,
        groupName: String,
    ): List<TagGroup> = tags.fold(initial = groups) { current, tag ->
        withTag(
            groups = current,
            tag = tag,
            groupName = groupName,
        )
    }

    /**
     * Takes [tag] out of its group. A group with no tags left goes away. Clearing the log never
     * calls this, so groups whose tags are not in the log now stay.
     */
    fun withoutTag(
        groups: List<TagGroup>,
        tag: String,
    ): List<TagGroup> = groups
        .map { group -> if (tag in group.tags) group.copy(tags = group.tags - tag) else group }
        .filter { it.tags.isNotEmpty() }

    /** Takes each of [tags] out of its group. */
    fun ungrouped(
        groups: List<TagGroup>,
        tags: List<String>,
    ): List<TagGroup> = tags.fold(initial = groups) { current, tag ->
        withoutTag(
            groups = current,
            tag = tag,
        )
    }

    /** The first free default name: [nameFor] 1, then 2, and so on. */
    fun nextName(
        groups: List<TagGroup>,
        nameFor: (Int) -> String,
    ): String = generateSequence(seed = 1) { it + 1 }
        .map(nameFor)
        .first { name -> groups.none { it.name == name } }

    /** Renames a group. A blank name or the name of another group changes nothing. */
    fun renamed(
        groups: List<TagGroup>,
        from: String,
        to: String,
    ): List<TagGroup> {
        val name = to.trim()
        if (name.isEmpty() || groups.any { it.name == name && it.name != from }) return groups
        return groups.map { if (it.name == from) it.copy(name = name) else it }
    }

    /** Deletes a group. Its tags stay in the panel, out of any group. */
    fun deleted(
        groups: List<TagGroup>,
        name: String,
    ): List<TagGroup> = groups.filter { it.name != name }

    fun groupOf(
        groups: List<TagGroup>,
        tag: String,
    ): TagGroup? = groups.firstOrNull { tag in it.tags }
}
