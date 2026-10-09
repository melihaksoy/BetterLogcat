package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * A cell in text selection mode: looks like a read-only text field (own background, outline) and
 * uses a strong selection color, so a drag selection is easy to see even on a selected row.
 * Shows the full text, never cut.
 */
@Composable
internal fun SelectableText(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    color: Color,
    textStyle: TextStyle,
    colors: LogColors,
) {
    val shape = RoundedCornerShape(size = 3.dp)
    val selectionColors = TextSelectionColors(
        handleColor = colors.link,
        backgroundColor = colors.textSelection,
    )
    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        SelectionContainer(
            modifier = modifier
                .background(
                    color = colors.fieldBackground,
                    shape = shape,
                )
                .border(
                    width = 1.dp,
                    color = colors.link,
                    shape = shape,
                )
                .padding(horizontal = 3.dp),
        ) {
            Text(
                text = text,
                color = color,
                style = textStyle,
                softWrap = true,
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun PreviewSelectableText() {
    BetterLogcatPreviewTheme {
        SelectableText(
            text = AnnotatedString(text = "Basket loaded with 3 items, total EUR 42.90"),
            color = JewelTheme.globalColors.text.normal,
            textStyle = JewelTheme.editorTextStyle,
            colors = rememberLogColors(),
        )
    }
}

// endregion
