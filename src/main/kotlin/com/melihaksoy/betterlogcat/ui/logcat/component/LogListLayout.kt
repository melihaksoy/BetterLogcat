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
        /** Splits text into pieces of [linesPerPiece] lines. Styles and links carry over. */
        fun split(
            text: AnnotatedString,
            linesPerPiece: Int,
        ): List<AnnotatedString> {
            val pieces = ArrayList<AnnotatedString>()
            var start = 0
            var lines = 0
            for (index in text.indices) {
                if (text[index] != '\n') continue
                if (++lines == linesPerPiece) {
                    pieces += text.subSequence(start, index)
                    start = index + 1
                    lines = 0
                }
            }
            pieces += text.subSequence(start, text.length)
            return pieces
        }
    }
}
