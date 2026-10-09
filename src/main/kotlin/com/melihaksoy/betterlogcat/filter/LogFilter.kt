package com.melihaksoy.betterlogcat.filter

import androidx.compose.runtime.Immutable
import com.melihaksoy.betterlogcat.model.LogLevel

/**
 * [query] is free text. [terms] are the `key:value` chips. Non-empty [includedTags] shows only
 * those tags and [excludedTags] always hides (both come from the Tags panel).
 */
@Immutable
data class LogFilter(
    val levels: Set<LogLevel> = LogLevel.entries.toSet(),
    val query: String = "",
    val isRegex: Boolean = false,
    val isMatchCase: Boolean = false,
    val terms: List<QueryTerm> = emptyList(),
    val includedTags: List<String> = emptyList(),
    val excludedTags: List<String> = emptyList(),
) {
    fun isTagVisible(tag: String): Boolean = (includedTags.isEmpty() || tag in includedTags) && tag !in excludedTags

    /** True when anything other than package terms narrows the list. */
    val hasNarrowing: Boolean
        get() {
            val running = effective()
            return running.query.isNotEmpty() ||
                levels.size != LogLevel.entries.size ||
                includedTags.isNotEmpty() ||
                excludedTags.isNotEmpty() ||
                running.terms.any { it.key != QueryKey.PACKAGE || it.isNegated }
        }

    val packageFilter: PackageFilter
        get() {
            val names = effective().terms.filter { it.key == QueryKey.PACKAGE && !it.isNegated }.map { it.value }
            return when {
                names.isEmpty() -> PackageFilter.All
                names.size > 1 -> PackageFilter.Several(names = names)
                names.single() == QueryTerm.MINE -> PackageFilter.ProjectApps
                else -> PackageFilter.Package(name = names.single())
            }
        }

    /** Replaces the positive `package:` terms in the search text with [packageFilter]. Other terms and text stay. */
    fun withPackageFilter(packageFilter: PackageFilter): LogFilter {
        val running = effective()
        val others = running.terms.filterNot { it.key == QueryKey.PACKAGE && !it.isNegated }
        val packageTerms = when (packageFilter) {
            PackageFilter.All -> emptyList()
            PackageFilter.ProjectApps -> listOf(QueryTerm.Mine)
            is PackageFilter.Package -> listOf(QueryTerm(key = QueryKey.PACKAGE, value = packageFilter.name))
            is PackageFilter.Several -> packageFilter.names.map { QueryTerm(key = QueryKey.PACKAGE, value = it) }
        }
        return withQueryString(text = join(terms = packageTerms + others, text = running.query))
    }

    fun withTerms(added: List<QueryTerm>): LogFilter = copy(terms = (terms + added).distinct())

    /** The whole filter as search text, the form saved in history and favorites. */
    fun toQueryString(): String = join(
        terms = terms,
        text = query,
    )

    /**
     * Sets the search text. Terms stay in the text, like Android Studio's Logcat: the field shows them
     * colored and they stay editable. A trailing space leaves the cursor ready for the next term.
     */
    fun withQueryString(text: String): LogFilter {
        val trimmed = text.trim()
        return copy(
            terms = emptyList(),
            query = if (trimmed.isEmpty()) "" else "$trimmed ",
        )
    }

    /**
     * The filter that actually runs: terms parsed out of the search text, the rest as free text.
     * A key still being typed at the end (`pack`, `package:`) is left out, so logs do not vanish mid-word.
     */
    fun effective(): LogFilter {
        val parsed = QueryParser.parseAll(text = query)
        var freeText = parsed.remainingText.trim()
        if (query.isNotEmpty() && !query.last().isWhitespace()) {
            val lastWord = freeText.substringAfterLast(' ')
            if (isKeyInProgress(word = lastWord)) freeText = freeText.dropLast(n = lastWord.length).trim()
        }
        if (parsed.terms.isEmpty() && freeText == query) return this
        return copy(
            terms = (terms + parsed.terms).distinct(),
            query = freeText,
        )
    }

    private fun isKeyInProgress(word: String): Boolean {
        val body = word.removePrefix("-")
        if (body.isEmpty()) return word == "-"
        if (body.endsWith(':')) return QueryKey.fromToken(token = body.dropLast(n = 1).trimEnd('=', '~')) != null
        return QueryKey.entries.any { it.token.startsWith(body) }
    }

    private fun join(
        terms: List<QueryTerm>,
        text: String,
    ): String = (terms.map { it.text } + text.trim()).filter { it.isNotEmpty() }.joinToString(separator = " ")
}
