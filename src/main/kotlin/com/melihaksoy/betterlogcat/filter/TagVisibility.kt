package com.melihaksoy.betterlogcat.filter

/**
 * Show and hide tags in a [LogFilter]. Rules:
 * - With no include list, hiding adds to [LogFilter.excludedTags].
 * - With an include list ("show only" mode), showing adds to it and hiding removes from it.
 *   Removing the last included tag hides every known tag, so nothing hidden comes back.
 */
object TagVisibility {

    fun setVisible(
        filter: LogFilter,
        tags: Collection<String>,
        isVisible: Boolean,
        knownTags: Collection<String>,
    ): LogFilter {
        if (tags.isEmpty()) return filter
        return if (isVisible) {
            filter.copy(
                excludedTags = filter.excludedTags - tags.toSet(),
                includedTags = if (filter.includedTags.isEmpty()) filter.includedTags else (filter.includedTags + tags).distinct(),
            )
        } else if (filter.includedTags.isEmpty()) {
            filter.copy(excludedTags = (filter.excludedTags + tags).distinct())
        } else {
            val included = filter.includedTags - tags.toSet()
            if (included.isEmpty()) {
                filter.copy(
                    includedTags = emptyList(),
                    excludedTags = (filter.excludedTags + knownTags).distinct(),
                )
            } else {
                filter.copy(includedTags = included)
            }
        }
    }

    fun showOnly(
        filter: LogFilter,
        tag: String,
    ): LogFilter = filter.copy(
        includedTags = listOf(tag),
        excludedTags = filter.excludedTags - tag,
    )

    fun showAll(filter: LogFilter): LogFilter = filter.copy(
        includedTags = emptyList(),
        excludedTags = emptyList(),
    )
}
