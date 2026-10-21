package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text

/** One JSON block around a line: its place among the blocks that can fold, and its key (or `[3]` in an array). */
@Immutable
internal data class JsonPathItem(
    val block: Int,
    val label: String,
)

/** Where a JSON block starts in split message text: the piece, and the line inside that piece. */
internal data class JsonBlockPlace(
    val piece: Int,
    val line: Int,
)

/** Reads the block annotations that [buildMessageText] puts on pretty JSON. */
internal object JsonPath {
    const val TAG = "json-block"

    fun encode(item: JsonPathItem): String = "${item.block}:${item.label}"

    private fun decode(annotation: String): JsonPathItem? {
        val block = annotation.substringBefore(':').toIntOrNull() ?: return null
        return JsonPathItem(
            block = block,
            label = annotation.substringAfter(':'),
        )
    }

    /** The blocks around [line] of [text], outer first. */
    fun at(
        text: AnnotatedString,
        line: Int,
    ): List<JsonPathItem> {
        if (text.isEmpty()) return emptyList()
        var start = 0
        for (skipped in 0 until line) {
            val next = text.indexOf(char = '\n', startIndex = start)
            if (next < 0) break
            start = next + 1
        }
        // The first character of the line, so a line that opens a block is inside that block.
        while (start < text.length - 1 && text[start] == ' ') start++
        val offset = start.coerceAtMost(maximumValue = text.length - 1)
        return text.getStringAnnotations(
            tag = TAG,
            start = offset,
            end = offset + 1,
        ).mapNotNull { decode(annotation = it.item) }.sortedBy { it.block }
    }

    /** Where [block] starts in [pieces], or null when no piece shows it (it is inside a folded block). */
    fun find(
        pieces: List<AnnotatedString>,
        block: Int,
    ): JsonBlockPlace? {
        pieces.forEachIndexed { index, piece ->
            val range = piece.getStringAnnotations(
                tag = TAG,
                start = 0,
                end = piece.length,
            ).firstOrNull { decode(annotation = it.item)?.block == block }
            if (range != null) {
                return JsonBlockPlace(
                    piece = index,
                    line = piece.text.take(n = range.start).count { it == '\n' },
                )
            }
        }
        return null
    }
}

/**
 * Pinned at the top while you scroll inside a big JSON block: the keys of the blocks around the top line,
 * outer first. A click on a key folds that block, so you do not have to scroll back up to its first line.
 */
@Composable
internal fun JsonPathBar(
    modifier: Modifier = Modifier,
    path: List<JsonPathItem>,
    colors: LogColors,
    textStyle: TextStyle,
    onFold: (JsonPathItem) -> Unit,
) {
    // A deep path keeps its outer block and the inner blocks, the ones you are most likely to fold.
    val shown = if (path.size > MAX_SHOWN) listOf(path.first(), null) + path.takeLast(n = MAX_SHOWN - 1) else path
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = JewelTheme.globalColors.panelBackground)
            .padding(
                horizontal = 8.dp,
                vertical = 2.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Text(
            text = BetterLogcatBundle.message("row.jsonPath.fold"),
            color = colors.muted,
            style = textStyle,
        )
        shown.forEachIndexed { index, item ->
            if (index > 0) {
                Text(
                    text = "›",
                    color = colors.muted,
                    style = textStyle,
                )
            }
            if (item == null) {
                Text(
                    text = "…",
                    color = colors.muted,
                    style = textStyle,
                )
            } else {
                Link(
                    text = item.label,
                    onClick = { onFold(item) },
                )
            }
        }
    }
}

private const val MAX_SHOWN = 6

// region Previews

@Preview
@Composable
private fun PreviewJsonPathBar() {
    BetterLogcatPreviewTheme {
        JsonPathBar(
            path = listOf(
                JsonPathItem(
                    block = 0,
                    label = "{ }",
                ),
                JsonPathItem(
                    block = 1,
                    label = "results",
                ),
                JsonPathItem(
                    block = 2,
                    label = "[3]",
                ),
                JsonPathItem(
                    block = 5,
                    label = "abilities",
                ),
            ),
            colors = rememberLogColors(),
            textStyle = JewelTheme.editorTextStyle,
            onFold = {},
        )
    }
}

// endregion
