package com.melihaksoy.betterlogcat.filter

/**
 * Show and hide tags by editing the `tag` terms in the search text, so the choice is visible,
 * editable, and saved with the filter. The panel owns two terms:
 * - `-tag=:a,b` hides those tags.
 * - `tag=:a,b` ("show only" mode) shows only those tags.
 *
 * Tag terms the user typed with `:` or `~:` stay, except for [showOnly] and [showAll], which clear every tag term.
 */
object TagVisibility {

    /** Past this size, a hide list turns into a show list when that is shorter. */
    private const val MAX_HIDE_LIST = 12

    fun setVisible(
        filter: LogFilter,
        tags: Collection<String>,
        isVisible: Boolean,
        knownTags: Collection<String>,
    ): LogFilter {
        if (tags.isEmpty()) return filter
        val lists = TagLists.of(filter = filter)
        val shown = LinkedHashSet(lists.shown)
        val hidden = LinkedHashSet(lists.hidden)
        if (isVisible) {
            hidden -= tags.toSet()
            if (shown.isNotEmpty()) shown += tags
        } else if (shown.isEmpty()) {
            hidden += tags
        } else {
            shown -= tags.toSet()
            // Nothing left to show only: hide every known tag, so nothing hidden comes back.
            if (shown.isEmpty()) hidden += knownTags
        }
        if (shown.isEmpty() && hidden.size > MAX_HIDE_LIST) {
            val visible = knownTags.filter { it !in hidden }
            if (visible.isNotEmpty() && visible.size < hidden.size) {
                shown += visible
                hidden.clear()
            }
        }
        return filter.withRunningTerms(
            terms = lists.others + tagTerms(
                shown = shown,
                hidden = hidden,
            ),
        )
    }

    fun showOnly(
        filter: LogFilter,
        tag: String,
    ): LogFilter = filter.withRunningTerms(
        terms = withoutTagTerms(filter = filter) + tagTerms(
            shown = listOf(tag),
            hidden = emptyList(),
        ),
    )

    fun showAll(filter: LogFilter): LogFilter = filter.withRunningTerms(terms = withoutTagTerms(filter = filter))

    private fun withoutTagTerms(filter: LogFilter): List<QueryTerm> = filter.effective().terms.filter { it.key != QueryKey.TAG }

    private fun tagTerms(
        shown: Collection<String>,
        hidden: Collection<String>,
    ): List<QueryTerm> = listOfNotNull(
        shown.takeIf { it.isNotEmpty() }?.let {
            QueryTerm(
                key = QueryKey.TAG,
                value = it.joinToString(separator = ","),
                mode = MatchMode.EXACT,
            )
        },
        hidden.takeIf { it.isNotEmpty() }?.let {
            QueryTerm(
                key = QueryKey.TAG,
                value = it.joinToString(separator = ","),
                mode = MatchMode.EXACT,
                isNegated = true,
            )
        },
    )

    /** The panel's two lists, read from every `tag=:` term, and the other terms. */
    private class TagLists(
        val shown: List<String>,
        val hidden: List<String>,
        val others: List<QueryTerm>,
    ) {
        companion object {
            fun of(filter: LogFilter): TagLists {
                val (owned, others) = filter.effective().terms.partition { it.key == QueryKey.TAG && it.mode == MatchMode.EXACT }
                return TagLists(
                    shown = owned.filter { !it.isNegated }.flatMap { it.values() }.distinct(),
                    hidden = owned.filter { it.isNegated }.flatMap { it.values() }.distinct(),
                    others = others,
                )
            }

            private fun QueryTerm.values(): List<String> = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
}
