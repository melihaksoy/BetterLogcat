package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.ui.text.AnnotatedString
import com.melihaksoy.betterlogcat.model.LogEntry

/** A lazy list item: the row of entry [entryIndex] when [chunk] is 0, else one more piece of its message. */
internal data class ItemRef(
    val entryIndex: Int,
    val chunk: Int,
)

/**
 * Maps lazy list items to entries. A big expanded message is split into pieces of a few lines; the first
 * piece shows in the entry's row and every other piece is one more list item under it. So the list only
 * lays out and draws the visible pieces, and scrolling a huge JSON body stays smooth.
 *
 * [chunked] maps entry index to its message pieces; it holds only split entries.
 */
internal class LogListLayout(
    val entries: List<LogEntry>,
    val chunked: Map<Int, List<AnnotatedString>>,
) {
    private val splitIndices: IntArray = chunked.keys.sorted().toIntArray()

    /** Extra items (pieces after the first) of each split entry, in [splitIndices] order. */
    private val extras: IntArray = IntArray(splitIndices.size) { chunked.getValue(splitIndices[it]).size - 1 }

    /** Extra items before split entry k (exclusive). */
    private val extrasBefore: IntArray = IntArray(splitIndices.size).also { before ->
        var sum = 0
        for (k in splitIndices.indices) {
            before[k] = sum
            sum += extras[k]
        }
    }

    val itemCount: Int = entries.size + extras.sum()

    fun itemAt(item: Int): ItemRef {
        // Last split entry whose row is at or before this item.
        var low = 0
        var high = splitIndices.size - 1
        var found = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (rowItemOf(k = middle) <= item) {
                found = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        if (found < 0) {
            return ItemRef(
                entryIndex = item,
                chunk = 0,
            )
        }
        val row = rowItemOf(k = found)
        return if (item <= row + extras[found]) {
            ItemRef(
                entryIndex = splitIndices[found],
                chunk = item - row,
            )
        } else {
            ItemRef(
                entryIndex = item - extrasBefore[found] - extras[found],
                chunk = 0,
            )
        }
    }

    /** The item of the entry's row. */
    fun firstItemOf(entryIndex: Int): Int {
        var low = 0
        var high = splitIndices.size - 1
        var before = 0
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (splitIndices[middle] < entryIndex) {
                before = extrasBefore[middle] + extras[middle]
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return entryIndex + before
    }

    private fun rowItemOf(k: Int): Int = splitIndices[k] + extrasBefore[k]

    companion object {
        /** Longest piece in characters. One huge line (minified JSON, a dump) is cut too, so it never lays out at once. */
        const val MAX_PIECE_CHARS = 3_000

        /** How far back a cut looks for a space or comma, so words and values stay whole. */
        private const val SOFT_CUT_WINDOW = 200

        /** Splits text into pieces of [linesPerPiece] lines and at most [maxChars] characters. Styles and links carry over. */
        fun split(
            text: AnnotatedString,
            linesPerPiece: Int,
            maxChars: Int = MAX_PIECE_CHARS,
        ): List<AnnotatedString> {
            val pieces = ArrayList<AnnotatedString>()
            var start = 0
            var lines = 0
            var index = 0
            while (index < text.length) {
                if (text[index] == '\n') {
                    if (++lines == linesPerPiece) {
                        pieces += text.subSequence(start, index)
                        start = index + 1
                        lines = 0
                    }
                } else if (index - start >= maxChars) {
                    val cut = softCut(
                        text = text,
                        start = start,
                        index = index,
                    )
                    pieces += text.subSequence(start, cut)
                    start = cut
                    lines = 0
                }
                index++
            }
            pieces += text.subSequence(start, text.length)
            return pieces
        }

        /** End of a piece cut near [index]: just after the last space or comma close by, else [index]. */
        private fun softCut(
            text: AnnotatedString,
            start: Int,
            index: Int,
        ): Int {
            val floor = maxOf(start + 1, index - SOFT_CUT_WINDOW)
            for (position in index downTo floor) {
                if (text[position] == ' ' || text[position] == ',') return position + 1
            }
            return index
        }
    }
}
