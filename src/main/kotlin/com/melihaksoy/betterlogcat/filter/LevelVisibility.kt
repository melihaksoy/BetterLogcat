package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogLevel

/**
 * The V D I W E A toggles edit the `level` terms in the search text, so the choice is visible, editable
 * and saved with the filter, like the Tags panel. The shortest form wins:
 * - W and up is `level:W`, as in Android Studio.
 * - Else `-level=:V,I` hides some levels, or `level=:E,A` shows only some.
 */
object LevelVisibility {

    /** Levels the toggles show as on: what the level toggles and the `level` terms let through. */
    fun visible(filter: LogFilter): Set<LogLevel> {
        val matcher = LogFilterMatcher(
            filter = filter,
            processNames = emptyMap(),
        )
        return LogLevel.entries.filter { matcher.isLevelVisible(level = it) }.toSet()
    }

    /** Turns [level] on or off. The last level on stays on, so the list never goes empty by accident. */
    fun toggle(
        filter: LogFilter,
        level: LogLevel,
    ): LogFilter {
        val current = visible(filter = filter)
        if (level in current && current.size == 1) return filter
        return withVisible(
            filter = filter,
            levels = if (level in current) current - level else current + level,
        )
    }

    /** Replaces every `level` term with terms for [levels]. Clears the old level toggles, so the text says it all. */
    fun withVisible(
        filter: LogFilter,
        levels: Set<LogLevel>,
    ): LogFilter {
        val others = filter.effective().terms.filter { it.key != QueryKey.LEVEL }
        return filter.copy(levels = LogLevel.entries.toSet()).withRunningTerms(terms = others + levelTerms(levels = levels))
    }

    private fun levelTerms(levels: Set<LogLevel>): List<QueryTerm> {
        if (levels.isEmpty() || levels.size == LogLevel.entries.size) return emptyList()
        val lowest = levels.min()
        if (LogLevel.entries.filter { it >= lowest }.toSet() == levels) {
            return listOf(
                QueryTerm(
                    key = QueryKey.LEVEL,
                    value = lowest.letter.toString(),
                ),
            )
        }
        val hidden = LogLevel.entries.filter { it !in levels }
        val isHiding = hidden.size <= levels.size
        return listOf(
            QueryTerm(
                key = QueryKey.LEVEL,
                value = (if (isHiding) hidden else levels.sorted()).joinToString(separator = ",") { it.letter.toString() },
                mode = MatchMode.EXACT,
                isNegated = isHiding,
            ),
        )
    }
}
