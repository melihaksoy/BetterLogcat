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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.filter.QueryKey
import com.melihaksoy.betterlogcat.filter.QueryParser
import com.melihaksoy.betterlogcat.ui.logcat.model.SavedFiltersUiModel
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/** Favorites and history behind the funnel button, like Android Studio's Logcat. */
@Composable
internal fun SavedFiltersPopup(
    savedFilters: SavedFiltersUiModel,
    currentQuery: String,
    width: Dp,
    offsetY: Int,
    colors: LogColors,
    onApply: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        offset = IntOffset(
            x = 0,
            y = offsetY,
        ),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        SavedFiltersList(
            modifier = Modifier.width(width),
            savedFilters = savedFilters,
            currentQuery = currentQuery,
            colors = colors,
            onApply = onApply,
            onToggleFavorite = onToggleFavorite,
            onRemove = onRemove,
        )
    }
}

@Composable
private fun SavedFiltersList(
    modifier: Modifier = Modifier,
    savedFilters: SavedFiltersUiModel,
    currentQuery: String,
    colors: LogColors,
    onApply: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val shape = RoundedCornerShape(size = 6.dp)
    Column(
        modifier = modifier
            .heightIn(max = 420.dp)
            .background(
                color = JewelTheme.globalColors.panelBackground,
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = JewelTheme.globalColors.borders.normal,
                shape = shape,
            )
            .verticalScroll(state = rememberScrollState())
            .padding(vertical = 4.dp),
    ) {
        if (currentQuery.isNotBlank() && currentQuery !in savedFilters.favorites) {
            ActionRow(
                text = BetterLogcatBundle.message("filters.saveCurrent"),
                colors = colors,
                onClick = { onToggleFavorite(currentQuery) },
            )
        }
        if (savedFilters.favorites.isEmpty() && savedFilters.history.isEmpty()) {
            Text(
                text = BetterLogcatBundle.message("filters.empty"),
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 6.dp,
                ),
                color = colors.muted,
            )
        }
        if (savedFilters.favorites.isNotEmpty()) {
            SectionTitle(text = BetterLogcatBundle.message("filters.favorites"))
            savedFilters.favorites.forEach { query ->
                SavedFilterRow(
                    query = query,
                    count = savedFilters.counts[query] ?: 0,
                    isFavorite = true,
                    isCurrent = query == currentQuery,
                    colors = colors,
                    onApply = { onApply(query) },
                    onToggleFavorite = { onToggleFavorite(query) },
                    onRemove = null,
                )
            }
        }
        if (savedFilters.history.isNotEmpty()) {
            SectionTitle(text = BetterLogcatBundle.message("filters.history"))
            savedFilters.history.forEach { query ->
                SavedFilterRow(
                    query = query,
                    count = savedFilters.counts[query] ?: 0,
                    isFavorite = false,
                    isCurrent = query == currentQuery,
                    colors = colors,
                    onApply = { onApply(query) },
                    onToggleFavorite = { onToggleFavorite(query) },
                    onRemove = { onRemove(query) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(
            start = 10.dp,
            end = 10.dp,
            top = 6.dp,
            bottom = 2.dp,
        ),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ActionRow(
    text: String,
    colors: LogColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Icon(
            key = AllIconsKeys.Nodes.NotFavoriteOnHover,
            contentDescription = null,
        )
        Text(
            text = text,
            color = colors.link,
        )
    }
}

/** A saved filter. Shows its `name:` when it has one, else the query. The count caps at 99+. */
@Composable
private fun SavedFilterRow(
    query: String,
    count: Int,
    isFavorite: Boolean,
    isCurrent: Boolean,
    colors: LogColors,
    onApply: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val name = remember(query) {
        QueryParser.parseAll(text = query).terms.firstOrNull { it.key == QueryKey.NAME }?.value.orEmpty()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .background(
                color = when {
                    isHovered -> colors.hover
                    isCurrent -> colors.selection
                    else -> Color.Transparent
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onApply,
            )
            .padding(
                horizontal = 10.dp,
                vertical = 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        ToolbarIconButton(
            iconKey = if (isFavorite) AllIconsKeys.Nodes.Favorite else AllIconsKeys.Nodes.NotFavoriteOnHover,
            tooltip = BetterLogcatBundle.message(if (isFavorite) "filters.unfavorite" else "filters.favorite"),
            onClick = onToggleFavorite,
        )
        Column(modifier = Modifier.weight(1f)) {
            if (name.isNotEmpty()) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = query,
                style = JewelTheme.editorTextStyle,
                color = if (name.isNotEmpty()) colors.muted else JewelTheme.globalColors.text.normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = if (count >= SavedFiltersUiModel.COUNT_LIMIT) "99+" else count.toString(),
            color = colors.muted,
        )
        if (onRemove != null) {
            ToolbarIconButton(
                modifier = Modifier.size(20.dp),
                iconKey = AllIconsKeys.General.CloseSmall,
                tooltip = BetterLogcatBundle.message("filters.remove"),
                onClick = onRemove,
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun PreviewSavedFilters() {
    BetterLogcatPreviewTheme {
        SavedFiltersList(
            modifier = Modifier.width(460.dp),
            savedFilters = SavedFiltersUiModel(
                favorites = listOf("name:Network package:mine tag:OkHttp"),
                history = listOf("package:mine -tag:art", "package:mine level:error"),
                counts = mapOf(
                    "name:Network package:mine tag:OkHttp" to 100,
                    "package:mine -tag:art" to 42,
                    "package:mine level:error" to 3,
                ),
            ),
            currentQuery = "package:mine -tag:art",
            colors = rememberLogColors(),
            onApply = {},
            onToggleFavorite = {},
            onRemove = {},
        )
    }
}

@Preview
@Composable
private fun PreviewSavedFiltersEmpty() {
    BetterLogcatPreviewTheme {
        SavedFiltersList(
            modifier = Modifier.width(460.dp),
            savedFilters = SavedFiltersUiModel(),
            currentQuery = "",
            colors = rememberLogColors(),
            onApply = {},
            onToggleFavorite = {},
            onRemove = {},
        )
    }
}

// endregion
