package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.filter.QueryKey
import com.melihaksoy.betterlogcat.filter.Suggestion
import com.melihaksoy.betterlogcat.filter.SuggestionList
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/**
 * Suggestion dropdown under the search field. Not focusable, so typing stays in the field;
 * the field moves [selectedIndex] with the arrow keys.
 */
@Composable
internal fun SearchSuggestionsPopup(
    suggestions: SuggestionList,
    selectedIndex: Int,
    width: Dp,
    offsetY: Int,
    colors: LogColors,
    onPick: (Suggestion) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        offset = IntOffset(
            x = 0,
            y = offsetY,
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        SuggestionsList(
            modifier = Modifier.width(width),
            suggestions = suggestions,
            selectedIndex = selectedIndex,
            colors = colors,
            onPick = onPick,
        )
    }
}

@Composable
private fun SuggestionsList(
    modifier: Modifier = Modifier,
    suggestions: SuggestionList,
    selectedIndex: Int,
    colors: LogColors,
    onPick: (Suggestion) -> Unit,
) {
    val shape = RoundedCornerShape(size = 6.dp)
    Column(
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
            .padding(vertical = 4.dp),
    ) {
        suggestions.items.forEachIndexed { index, suggestion ->
            SuggestionRow(
                suggestion = suggestion,
                isSelected = index == selectedIndex,
                colors = colors,
                onPick = { onPick(suggestion) },
            )
        }
        Text(
            text = BetterLogcatBundle.message("search.suggestions.hint"),
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 4.dp,
            ),
            color = colors.muted,
        )
    }
}

@Composable
private fun SuggestionRow(
    modifier: Modifier = Modifier,
    suggestion: Suggestion,
    isSelected: Boolean,
    colors: LogColors,
    onPick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .background(
                color = when {
                    isSelected -> colors.selection
                    isHovered -> colors.hover
                    else -> Color.Transparent
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPick,
            )
            .padding(
                horizontal = 10.dp,
                vertical = 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Text(
            text = suggestion.label,
            style = JewelTheme.editorTextStyle,
            color = if (suggestion.key != null) colors.link else JewelTheme.globalColors.text.normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        suggestion.key?.let { key ->
            Text(
                text = if (suggestion.isMine) BetterLogcatBundle.message("search.key.mine") else keyDescription(key = key),
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun keyDescription(key: QueryKey): String = when (key) {
    QueryKey.PACKAGE -> BetterLogcatBundle.message("search.key.package")
    QueryKey.TAG -> BetterLogcatBundle.message("search.key.tag")
    QueryKey.MESSAGE -> BetterLogcatBundle.message("search.key.message")
    QueryKey.LEVEL -> BetterLogcatBundle.message("search.key.level")
    QueryKey.PROCESS -> BetterLogcatBundle.message("search.key.process")
    QueryKey.PID -> BetterLogcatBundle.message("search.key.pid")
    QueryKey.AGE -> BetterLogcatBundle.message("search.key.age")
    QueryKey.CONTEXT -> BetterLogcatBundle.message("search.key.context")
    QueryKey.IS -> BetterLogcatBundle.message("search.key.is")
    QueryKey.LINE -> BetterLogcatBundle.message("search.key.line")
    QueryKey.NAME -> BetterLogcatBundle.message("search.key.name")
}

// region Previews

@Preview
@Composable
private fun PreviewSuggestionsKeys() {
    BetterLogcatPreviewTheme {
        SuggestionsList(
            modifier = Modifier.width(420.dp),
            suggestions = SuggestionList(
                tokenStart = 0,
                tokenEnd = 1,
                isTokenEmpty = false,
                items = listOf(QueryKey.PACKAGE, QueryKey.PID, QueryKey.PROCESS).map {
                    Suggestion(
                        label = "${it.token}:",
                        key = it,
                        replacement = "${it.token}:",
                    )
                },
            ),
            selectedIndex = 0,
            colors = rememberLogColors(),
            onPick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewSuggestionsValues() {
    BetterLogcatPreviewTheme {
        SuggestionsList(
            modifier = Modifier.width(420.dp),
            suggestions = SuggestionList(
                tokenStart = 0,
                tokenEnd = 8,
                isTokenEmpty = false,
                items = listOf("mine", "com.vintagegarage.app", "com.android.systemui").map {
                    Suggestion(
                        label = it,
                        key = null,
                        replacement = "package:$it ",
                    )
                },
            ),
            selectedIndex = 1,
            colors = rememberLogColors(),
            onPick = {},
        )
    }
}

// endregion
