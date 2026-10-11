package com.melihaksoy.betterlogcat.ui.logcat.model

import androidx.compose.runtime.Immutable

/**
 * The selected tags in the Tags panel, as in the IDE's project tree. Selection is for drag and drop
 * only; the checkbox shows or hides a tag. [anchor] is the tag a Shift range starts from.
 */
@Immutable
data class TagSelectionUiModel(
    val tags: Set<String> = emptySet(),
    val anchor: String? = null,
) {
    /**
     * A click on [tag]. [rows] are the tag rows on screen, top to bottom. A plain click selects only
     * [tag], [isToggle] (Cmd on macOS, Ctrl elsewhere) adds or removes it, and [isRange] (Shift)
     * selects the rows from the anchor to [tag].
     */
    fun clicked(
        tag: String,
        rows: List<String>,
        isToggle: Boolean,
        isRange: Boolean,
    ): TagSelectionUiModel {
        val from = anchor?.let { rows.indexOf(element = it) } ?: -1
        val to = rows.indexOf(element = tag)
        return when {
            isRange && from >= 0 && to >= 0 -> {
                val range = rows.subList(
                    fromIndex = minOf(from, to),
                    toIndex = maxOf(from, to) + 1,
                )
                copy(tags = if (isToggle) tags + range else range.toSet())
            }
            isToggle -> TagSelectionUiModel(
                tags = if (tag in tags) tags - tag else tags + tag,
                anchor = tag,
            )
            else -> TagSelectionUiModel(
                tags = setOf(tag),
                anchor = tag,
            )
        }
    }

    /**
     * The tags a drag from [tag] moves, in row order: every selected row when [tag] is selected,
     * else [tag] alone. Selected tags in a folded group are not on screen, so they stay.
     */
    fun dragged(
        tag: String,
        rows: List<String>,
    ): List<String> = if (tag in tags) rows.filter { it in tags } else listOf(tag)
}
