package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Find in the visible logs without changing the filters, like an editor's find bar.
 * Enter / Shift+Enter (or the arrows) move between matches; Esc closes.
 */
@Composable
internal fun FindBar(
    modifier: Modifier = Modifier,
    queryState: TextFieldState,
    matchCount: Int,
    currentMatch: Int,
    colors: LogColors,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val shape = RoundedCornerShape(size = 6.dp)
    Row(
        modifier = modifier
            .background(
                color = JewelTheme.globalColors.panelBackground,
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = JewelTheme.globalColors.borders.normal,
                shape = shape,
            )
            .padding(
                horizontal = 6.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        TextField(
            state = queryState,
            modifier = Modifier
                .width(260.dp)
                .focusRequester(focusRequester = focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            if (event.isShiftPressed) onPrevious() else onNext()
                            true
                        }
                        Key.Escape -> {
                            onClose()
                            true
                        }
                        else -> false
                    }
                },
            placeholder = { Text(text = BetterLogcatBundle.message("find.placeholder")) },
            leadingIcon = {
                Icon(
                    key = AllIconsKeys.Actions.Find,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
            },
        )
        Text(
            text = when {
                queryState.text.isEmpty() -> ""
                matchCount == 0 -> BetterLogcatBundle.message("find.none")
                else -> BetterLogcatBundle.message("find.count", currentMatch + 1, matchCount)
            },
            modifier = Modifier.width(72.dp),
            color = if (matchCount == 0 && queryState.text.isNotEmpty()) colors.error else colors.muted,
        )
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.PreviousOccurence,
            tooltip = BetterLogcatBundle.message("find.previous"),
            isEnabled = matchCount > 0,
            onClick = onPrevious,
        )
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.NextOccurence,
            tooltip = BetterLogcatBundle.message("find.next"),
            isEnabled = matchCount > 0,
            onClick = onNext,
        )
        ToolbarIconButton(
            iconKey = AllIconsKeys.General.Close,
            tooltip = BetterLogcatBundle.message("find.close"),
            onClick = onClose,
        )
    }
}

// region Previews

@Preview
@Composable
private fun PreviewFindBar() {
    BetterLogcatPreviewTheme {
        FindBar(
            queryState = rememberTextFieldState(initialText = "timeout"),
            matchCount = 42,
            currentMatch = 2,
            colors = rememberLogColors(),
            onNext = {},
            onPrevious = {},
            onClose = {},
        )
    }
}

@Preview
@Composable
private fun PreviewFindBarNoMatch() {
    BetterLogcatPreviewTheme {
        FindBar(
            queryState = rememberTextFieldState(initialText = "nothing here"),
            matchCount = 0,
            currentMatch = 0,
            colors = rememberLogColors(),
            onNext = {},
            onPrevious = {},
            onClose = {},
        )
    }
}

// endregion
