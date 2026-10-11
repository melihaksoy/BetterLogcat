package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogLevel

/** What the suggestion list can offer: running app packages, tags (busiest first) and process names. */
data class SuggestionContext(
    val packages: List<String>,
    val tags: List<String>,
    val processNames: List<String>,
)

/** [replacement] replaces the token under the cursor. A value replacement ends with a space, so it becomes a chip. */
data class Suggestion(
    val label: String,
    val key: QueryKey?,
    val replacement: String,
    val isMine: Boolean = false,
)

data class SuggestionList(
    val tokenStart: Int,
    val tokenEnd: Int,
    val isTokenEmpty: Boolean,
    val items: List<Suggestion>,
) {
    companion object {
        val Empty = SuggestionList(
            tokenStart = 0,
            tokenEnd = 0,
            isTokenEmpty = true,
            items = emptyList(),
        )
    }
}

/**
 * Autocomplete for the search field, like Android Studio's Logcat:
 * a bare word suggests keys (`pa` → `package:`), `key:` suggests values (`package:` → `mine`, running apps).
 */
object QuerySuggestions {

    private const val MAX_ITEMS = 12
    private val ageValues = listOf("30s", "1m", "5m", "15m", "1h", "1d")
    private val isValues = listOf("crash", "anr", "exception", "strictmode", "problem", "stacktrace", "json")
    private val contextValues = listOf("1", "3", "5", "10")

    fun suggest(
        text: String,
        cursor: Int,
        context: SuggestionContext,
    ): SuggestionList {
        val end = cursor.coerceIn(minimumValue = 0, maximumValue = text.length)
        val start = text.lastIndexOfAny(chars = charArrayOf(' ', '\t', '\n'), startIndex = end - 1) + 1
        val token = text.substring(start, end)
        if (token.startsWith('"') || token.count { it == '"' } == 1) return SuggestionList.Empty
        val negation = if (token.startsWith('-')) "-" else ""
        val body = token.removePrefix("-")
        val colon = body.indexOf(':')
        val items = if (colon < 0) {
            keySuggestions(
                prefix = body,
                negation = negation,
            )
        } else {
            valueSuggestions(
                keyPart = body.substring(0, colon),
                prefix = body.substring(colon + 1),
                negation = negation,
                context = context,
            )
        }
        return SuggestionList(
            tokenStart = start,
            tokenEnd = end,
            isTokenEmpty = token.isEmpty(),
            items = items,
        )
    }

    /** Keys in alphabetical order, with `package:mine` right after `package:`, like Android Studio. */
    private fun keySuggestions(
        prefix: String,
        negation: String,
    ): List<Suggestion> = QueryKey.entries
        .filter { it.token.startsWith(prefix, ignoreCase = true) }
        .flatMap { key ->
            val keySuggestion = Suggestion(
                label = "${key.token}:",
                key = key,
                replacement = "$negation${key.token}:",
            )
            if (key == QueryKey.PACKAGE && negation.isEmpty()) {
                listOf(
                    keySuggestion,
                    Suggestion(
                        label = QueryTerm.Mine.text,
                        key = key,
                        replacement = "${QueryTerm.Mine.text} ",
                        isMine = true,
                    ),
                )
            } else {
                listOf(keySuggestion)
            }
        }

    private fun valueSuggestions(
        keyPart: String,
        prefix: String,
        negation: String,
        context: SuggestionContext,
    ): List<Suggestion> {
        val key = QueryKey.fromToken(token = keyPart.trimEnd('=', '~')) ?: return emptyList()
        val candidates = when (key) {
            QueryKey.PACKAGE -> listOf(QueryTerm.MINE) + context.packages
            QueryKey.TAG -> context.tags
            QueryKey.LEVEL -> LogLevel.entries.map { it.name.lowercase() }
            QueryKey.IS -> isValues
            QueryKey.AGE -> ageValues
            QueryKey.CONTEXT -> contextValues
            QueryKey.PROCESS -> context.processNames
            QueryKey.MESSAGE, QueryKey.PID, QueryKey.LINE, QueryKey.NAME -> emptyList()
        }
        val starts = candidates.filter { it.startsWith(prefix, ignoreCase = true) }
        val contains = candidates.filter { !it.startsWith(prefix, ignoreCase = true) && it.contains(prefix, ignoreCase = true) }
        return (starts + contains)
            .distinct()
            .take(MAX_ITEMS)
            .map { value ->
                Suggestion(
                    label = value,
                    key = null,
                    replacement = "$negation$keyPart:${quote(value = value)} ",
                )
            }
    }

    private fun quote(value: String): String = if (value.any { it.isWhitespace() }) "\"$value\"" else value
}
