package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.LogFilterMatcher
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.preview.LogcatPreviewData
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Side panel to show or hide tags one by one. Rows sort by entry count, busiest first.
 * "Show all" and "Hide all" act on the rows the search leaves.
 */
@Composable
internal fun TagsPanel(
    modifier: Modifier = Modifier,
    tagCounts: Map<String, Int>,
    tagRates: Map<String, IntArray> = emptyMap(),
    appTags: Set<String>,
    filter: LogFilter,
    colors: LogColors,
    onIntent: (LogcatIntent) -> Unit,
) {
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString().trim()
    val rows = remember(tagCounts, query) {
        tagCounts.entries
            .filter { query.isEmpty() || it.key.contains(other = query, ignoreCase = true) }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { it.key to it.value }
    }
    val tagMatcher = remember(filter) {
        LogFilterMatcher(
            filter = filter,
            processNames = emptyMap(),
        )
    }
    val hiddenCount = tagCounts.keys.count { !tagMatcher.isTagVisible(tag = it) }

    Column(modifier = modifier.background(color = JewelTheme.globalColors.panelBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 10.dp,
                    end = 4.dp,
                    top = 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = BetterLogcatBundle.message("tags.title"),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            ToolbarIconButton(
                iconKey = AllIconsKeys.General.Close,
                tooltip = BetterLogcatBundle.message("tags.close"),
                onClick = { onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.TAGS)) },
            )
        }
        TextField(
            state = searchState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                ),
            placeholder = { Text(text = BetterLogcatBundle.message("tags.search")) },
            leadingIcon = {
                Icon(
                    key = AllIconsKeys.Actions.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 2.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Link(
                text = BetterLogcatBundle.message("tags.showAll"),
                onClick = {
                    onIntent(
                        if (query.isEmpty()) {
                            LogcatIntent.ShowAllTags
                        } else {
                            LogcatIntent.SetTagsVisible(
                                tags = rows.map { it.first },
                                isVisible = true,
                            )
                        },
                    )
                },
            )
            Link(
                text = BetterLogcatBundle.message("tags.hideAll"),
                onClick = {
                    onIntent(
                        LogcatIntent.SetTagsVisible(
                            tags = rows.map { it.first },
                            isVisible = false,
                        ),
                    )
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            if (hiddenCount > 0) {
                Text(
                    text = BetterLogcatBundle.message("tags.hiddenCount", hiddenCount),
                    color = colors.muted,
                )
            }
        }
        if (rows.isEmpty()) {
            Text(
                text = BetterLogcatBundle.message(if (tagCounts.isEmpty()) "tags.empty" else "tags.noMatch"),
                modifier = Modifier.padding(all = 10.dp),
                color = colors.muted,
            )
        } else {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                ) {
                    // With known project apps, tags split into "Your app" and "Other" (system and other apps).
                    val groups = if (appTags.isEmpty()) {
                        listOf(TagGroup.ALL to rows)
                    } else {
                        listOf(
                            TagGroup.APP to rows.filter { it.first in appTags },
                            TagGroup.OTHER to rows.filter { it.first !in appTags },
                        ).filter { it.second.isNotEmpty() }
                    }
                    groups.forEach { (group, groupRows) ->
                        if (group != TagGroup.ALL) {
                            item(key = "header-${group.name}") {
                                GroupHeaderRow(
                                    title = BetterLogcatBundle.message(
                                        if (group == TagGroup.APP) "tags.group.app" else "tags.group.other",
                                        groupRows.size,
                                    ),
                                    colors = colors,
                                    onShowAll = {
                                        onIntent(
                                            LogcatIntent.SetTagsVisible(
                                                tags = groupRows.map { it.first },
                                                isVisible = true,
                                            ),
                                        )
                                    },
                                    onHideAll = {
                                        onIntent(
                                            LogcatIntent.SetTagsVisible(
                                                tags = groupRows.map { it.first },
                                                isVisible = false,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                        items(
                            items = groupRows,
                            key = { it.first },
                        ) { (tag, count) ->
                            TagRow(
                                tag = tag,
                                count = count,
                                rates = tagRates[tag],
                                isVisible = tagMatcher.isTagVisible(tag = tag),
                                colors = colors,
                                onVisibleChange = { isVisible ->
                                    onIntent(
                                        LogcatIntent.SetTagsVisible(
                                            tags = listOf(tag),
                                            isVisible = isVisible,
                                        ),
                                    )
                                },
                                onShowOnly = { onIntent(LogcatIntent.ShowOnlyTag(tag = tag)) },
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    scrollState = listState,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

private enum class TagGroup {
    ALL,
    APP,
    OTHER,
}

@Composable
private fun GroupHeaderRow(
    title: String,
    colors: LogColors,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colors.headerBackground)
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
        )
        Link(
            text = BetterLogcatBundle.message("tags.showAll"),
            onClick = onShowAll,
        )
        Link(
            text = BetterLogcatBundle.message("tags.hideAll"),
            onClick = onHideAll,
        )
    }
}

@Composable
private fun TagRow(
    modifier: Modifier = Modifier,
    tag: String,
    count: Int,
    rates: IntArray?,
    isVisible: Boolean,
    colors: LogColors,
    onVisibleChange: (Boolean) -> Unit,
    onShowOnly: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .hoverable(interactionSource = interactionSource)
            .background(color = if (isHovered) colors.hover else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onVisibleChange(!isVisible) },
            )
            .padding(
                start = 8.dp,
                end = 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Checkbox(
            checked = isVisible,
            onCheckedChange = onVisibleChange,
        )
        Text(
            text = tag,
            modifier = Modifier.weight(1f),
            color = if (isVisible) colors.tag(tag = tag) else colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isHovered) {
            Link(
                text = BetterLogcatBundle.message("tags.only"),
                onClick = onShowOnly,
            )
        }
        if (rates != null) {
            Sparkline(
                values = rates,
                color = if (isVisible) colors.tag(tag = tag) else colors.muted,
            )
        }
        Text(
            text = count.toString(),
            color = colors.muted,
        )
    }
}

/** Entries per second over the last 30 seconds, as small bars. Busy tags stand out at a glance. */
@Composable
private fun Sparkline(
    values: IntArray,
    color: Color,
) {
    val peak = (values.maxOrNull() ?: 0).coerceAtLeast(minimumValue = 1)
    Canvas(
        modifier = Modifier
            .width(SPARKLINE_WIDTH)
            .height(12.dp),
    ) {
        val barWidth = size.width / values.size
        values.forEachIndexed { index, value ->
            if (value == 0) return@forEachIndexed
            val barHeight = size.height * value / peak
            drawRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(x = index * barWidth, y = size.height - barHeight),
                size = Size(width = (barWidth - 1f).coerceAtLeast(minimumValue = 1f), height = barHeight),
            )
        }
    }
}

private val SPARKLINE_WIDTH = 44.dp

// region Previews

@Preview
@Composable
private fun PreviewTagsPanel() {
    BetterLogcatPreviewTheme {
        TagsPanel(
            tagCounts = LogcatPreviewData.tagCounts,
            appTags = setOf("OkHttp", "GarageApp", "AndroidRuntime"),
            filter = LogFilter(query = "package:mine -tag=:art,adbd "),
            colors = rememberLogColors(),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewTagsPanelEmpty() {
    BetterLogcatPreviewTheme {
        TagsPanel(
            tagCounts = emptyMap(),
            appTags = emptySet(),
            filter = LogFilter(),
            colors = rememberLogColors(),
            onIntent = {},
        )
    }
}

// endregion
