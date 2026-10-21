package com.melihaksoy.betterlogcat.filter

/** One long term value shown short: [range] of the search text shows as [text]. */
data class TermFold(
    val range: IntRange,
    val text: String,
)

/**
 * Long comma lists, such as the Tags panel's `tag=:a,b,c,…`, make the search field unreadable. The field
 * shows such a value as its first values and a count (`tag=:System.out,OkHttp +31`). The term under the
 * cursor shows in full, so it stays editable.
 */
object TermFolding {

    private const val MIN_FOLD_CHARS = 40
    private const val SHOWN_VALUES = 2

    /** Folds for [text]. [cursor] is -1 when the field has no focus; then every long term folds. */
    fun folds(
        text: String,
        cursor: Int,
    ): List<TermFold> = QueryParser.termRanges(text = text).mapNotNull { (range, term) ->
        if (cursor in range.first..range.last + 1) return@mapNotNull null
        val values = term.value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (values.size <= SHOWN_VALUES || term.value.length < MIN_FOLD_CHARS) return@mapNotNull null
        val valueStart = text.indexOf(':', startIndex = range.first) + 1
        TermFold(
            range = valueStart..range.last,
            text = values.take(n = SHOWN_VALUES).joinToString(separator = ",") + " +${values.size - SHOWN_VALUES}",
        )
    }
}
