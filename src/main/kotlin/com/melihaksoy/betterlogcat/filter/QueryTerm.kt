package com.melihaksoy.betterlogcat.filter

import androidx.compose.runtime.Immutable

/** Keys of `key:value` search terms, as in Android Studio's Logcat. Alphabetical, for the suggestion list. */
enum class QueryKey(val token: String) {
    AGE(token = "age"),

    /** Shows this many lines around each match, like `grep -C`. Does not filter. */
    CONTEXT(token = "context"),
    IS(token = "is"),
    LEVEL(token = "level"),
    LINE(token = "line"),
    MESSAGE(token = "message"),

    /** Names a saved filter. Does not filter. */
    NAME(token = "name"),
    PACKAGE(token = "package"),
    PID(token = "pid"),
    PROCESS(token = "process"),
    TAG(token = "tag"),
    ;

    companion object {
        fun fromToken(token: String): QueryKey? = entries.firstOrNull { it.token == token }
    }
}

/** `key:` contains (ignores case), `key=:` equals, `key~:` regex. */
enum class MatchMode(val token: String) {
    CONTAINS(token = ""),
    EXACT(token = "="),
    REGEX(token = "~"),
}

/**
 * One search chip, for example `package:mine`, `-tag:art` or `tag~:Ok.*`.
 * Terms with the same key combine with OR; different keys combine with AND; a `-` term excludes.
 */
@Immutable
data class QueryTerm(
    val key: QueryKey,
    val value: String,
    val mode: MatchMode = MatchMode.CONTAINS,
    val isNegated: Boolean = false,
) {
    /** The term as the user types it. */
    val text: String
        get() = buildString {
            if (isNegated) append('-')
            append(key.token)
            append(mode.token)
            append(':')
            if (value.any { it.isWhitespace() }) append('"').append(value).append('"') else append(value)
        }

    companion object {
        const val MINE = "mine"

        val Mine = QueryTerm(
            key = QueryKey.PACKAGE,
            value = MINE,
        )
    }
}
