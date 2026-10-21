package com.melihaksoy.betterlogcat.format

/**
 * A `{…}` or `[…]` block of pretty JSON that spans lines, so it can fold. [open] and [close] index its
 * brackets; [children] counts its keys or items. [index] is its place among the children of the block
 * around it, so an array item can show as `[3]`.
 */
data class JsonFoldRegion(
    val open: Int,
    val close: Int,
    val children: Int,
    val isArray: Boolean,
    val index: Int = 0,
)

/** Finds the blocks of pretty JSON that can fold. Brackets inside strings do not count. */
object JsonFolding {

    /** Blocks between [start] and [end] of [text] that span lines, in order of their opening bracket. */
    fun regions(
        text: CharSequence,
        start: Int = 0,
        end: Int = text.length,
    ): List<JsonFoldRegion> {
        val regions = ArrayList<JsonFoldRegion>()
        val open = ArrayList<Frame>()
        var isInString = false
        var isEscaped = false
        for (index in start until end) {
            val char = text[index]
            if (isInString) {
                when {
                    isEscaped -> isEscaped = false
                    char == '\\' -> isEscaped = true
                    char == '"' -> isInString = false
                }
                continue
            }
            when (char) {
                '"' -> {
                    isInString = true
                    open.lastOrNull()?.hasContent = true
                }
                '{', '[' -> {
                    open.lastOrNull()?.hasContent = true
                    open += Frame(
                        start = index,
                        index = open.lastOrNull()?.commas ?: 0,
                    )
                }
                '}', ']' -> {
                    val frame = open.removeLastOrNull() ?: continue
                    if (frame.hasNewline) {
                        open.lastOrNull()?.hasNewline = true
                        regions += JsonFoldRegion(
                            open = frame.start,
                            close = index,
                            children = if (frame.hasContent) frame.commas + 1 else 0,
                            isArray = text[frame.start] == '[',
                            index = frame.index,
                        )
                    }
                }
                ',' -> open.lastOrNull()?.let { it.commas++ }
                '\n' -> open.lastOrNull()?.hasNewline = true
                else -> if (!char.isWhitespace()) open.lastOrNull()?.hasContent = true
            }
        }
        return regions.sortedBy { it.open }
    }

    private class Frame(
        val start: Int,
        val index: Int,
    ) {
        var commas = 0
        var hasContent = false
        var hasNewline = false
    }
}
