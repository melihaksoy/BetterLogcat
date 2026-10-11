package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * A cell in text selection mode: looks like a read-only text field (own background, outline) and
 * uses a strong selection color, so a drag selection is easy to see even on a selected row.
 * Shows the full text, never cut.
 *
 * It is an editable field that drops every edit: Compose draws no caret in a read-only field, and the
 * caret shows where a click landed and lets Shift+arrows grow the selection, as in an editor.
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
    var value by remember(text) { mutableStateOf(value = TextFieldValue(annotatedString = text)) }
    val focusRequester = remember { FocusRequester() }
    // Focus at once, so the caret shows without a second click.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        BasicTextField(
            value = value,
            onValueChange = { new ->
                // Caret moves and selections only. Typing, paste and cut keep the log text as it is.
                if (new.text == value.text) {
                    value = value.copy(
                        selection = new.selection,
                        composition = null,
                    )
                }
            },
            modifier = modifier
                .focusRequester(focusRequester = focusRequester)
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
            textStyle = textStyle.merge(color = color),
            cursorBrush = SolidColor(value = JewelTheme.globalColors.text.normal),
        )
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
