package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * One JSON part of a message, from [start] to [end] (exclusive). [isComplete] is false for a body the
 * logger cut off: it gets colors but no pretty print. [isInline] marks a small value inside a line,
 * such as a header (`-> nel: {...}`), which stays on its line.
 */
@Immutable
data class JsonBlock(
    val start: Int,
    val end: Int,
    val raw: String,
    val pretty: String,
    val minified: String,
    val isComplete: Boolean,
    val isInline: Boolean,
)

@Stable
sealed interface JsonContent {

    @Immutable
    data object None : JsonContent

    /** [main] is the largest part: usually the response body. [prefix] and [suffix] surround it. */
    @Immutable
    data class Found(
        val blocks: List<JsonBlock>,
        val main: JsonBlock,
        val prefix: String,
        val suffix: String,
    ) : JsonContent {
        val raw: String
            get() = main.raw

        val pretty: String
            get() = main.pretty

        val minified: String
            get() = main.minified
    }
}
