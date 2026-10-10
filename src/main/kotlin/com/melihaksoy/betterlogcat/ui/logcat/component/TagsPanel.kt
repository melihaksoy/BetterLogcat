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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import com.melihaksoy.betterlogcat.model.TagDescriptions
import com.melihaksoy.betterlogcat.model.TagGroup
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.component.TriStateCheckbox
import java.awt.Point

/**
 * Side panel to show or hide tags one by one. Rows sort by entry count, busiest first.
 * "Show all" and "Hide all" act on the rows the search leaves. The user's tag groups come first, as
 * folders; a tag shows a description on hover when there is one. Right-click opens the tag menu.
 */
@Composable
internal fun TagsPanel(
    modifier: Modifier = Modifier,
    tagCounts: Map<String, Int>,
    tagRates: Map<String, IntArray> = emptyMap(),
    appTags: Set<String>,
    filter: LogFilter,
    colors: LogColors,
    tagNotes: Map<String, String> = emptyMap(),
    tagGroups: List<TagGroup> = emptyList(),
    onTagMenu: (String, Point) -> Unit = { _, _ -> },
    onGroupMenu: (String, Point) -> Unit = { _, _ -> },
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
    // Folded by default: a group is there to put noise away. A search opens groups with matches.
    var expandedGroups by remember { mutableStateOf(value = emptySet<String>()) }

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
        val groupedTags = remember(tagGroups) { tagGroups.flatMapTo(destination = HashSet()) { it.tags } }
        val rowTags = remember(rows) { rows.mapTo(destination = HashSet()) { it.first } }
        val matchingGroups = tagGroups.filter { group ->
            query.isEmpty() || group.name.contains(other = query, ignoreCase = true) || group.tags.any { it in rowTags }
        }
        if (rows.isEmpty() && matchingGroups.isEmpty()) {
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
                    matchingGroups.forEach { tagGroup ->
                        // Present tags only, busiest first; all of them when the search matches the group name.
                        val isNameMatch = query.isNotEmpty() && tagGroup.name.contains(other = query, ignoreCase = true)
                        val groupRows = tagCounts.entries
                            .filter { it.key in tagGroup.tags && (query.isEmpty() || isNameMatch || it.key.contains(other = query, ignoreCase = true)) }
                            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
                            .map { it.key to it.value }
                        val isExpanded = tagGroup.name in expandedGroups || (query.isNotEmpty() && groupRows.isNotEmpty())
                        val visibleCount = tagGroup.tags.count { tagMatcher.isTagVisible(tag = it) }
                        item(key = GROUP_KEY_PREFIX + tagGroup.name) {
                            TagGroupRow(
                                name = tagGroup.name,
                                count = groupRows.sumOf { it.second },
                                visibility = when (visibleCount) {
                                    tagGroup.tags.size -> ToggleableState.On
                                    0 -> ToggleableState.Off
                                    else -> ToggleableState.Indeterminate
                                },
                                isExpanded = isExpanded,
                                colors = colors,
                                onToggleExpanded = {
                                    expandedGroups = if (tagGroup.name in expandedGroups) expandedGroups - tagGroup.name else expandedGroups + tagGroup.name
                                },
                                onVisibleChange = { isVisible ->
                                    onIntent(
                                        LogcatIntent.SetTagsVisible(
                                            tags = tagGroup.tags,
                                            isVisible = isVisible,
                                        ),
                                    )
                                },
                                onShowOnly = { onIntent(LogcatIntent.ShowOnlyTags(tags = tagGroup.tags)) },
                                onMenu = { point -> onGroupMenu(tagGroup.name, point) },
                            )
                        }
                        if (isExpanded) {
                            items(
                                items = groupRows,
                                key = { it.first },
                            ) { (tag, count) ->
                                TagRow(
                                    tag = tag,
                                    count = count,
                                    rates = tagRates[tag],
                                    description = TagDescriptions.describe(
                                        tag = tag,
                                        userNotes = tagNotes,
                                    ),
                                    isVisible = tagMatcher.isTagVisible(tag = tag),
                                    indent = GROUP_INDENT,
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
                                    onMenu = { point -> onTagMenu(tag, point) },
                                )
                            }
                        }
                    }
                    val ungroupedRows = rows.filter { it.first !in groupedTags }
                    // With known project apps, tags split into "Your app" and "Other" (system and other apps).
                    val groups = if (appTags.isEmpty()) {
                        listOf(TagSection.ALL to ungroupedRows)
                    } else {
                        listOf(
                            TagSection.APP to ungroupedRows.filter { it.first in appTags },
                            TagSection.OTHER to ungroupedRows.filter { it.first !in appTags },
                        ).filter { it.second.isNotEmpty() }
                    }
                    groups.forEach { (group, groupRows) ->
                        if (group != TagSection.ALL) {
                            item(key = "header-${group.name}") {
                                GroupHeaderRow(
                                    title = BetterLogcatBundle.message(
                                        if (group == TagSection.APP) "tags.group.app" else "tags.group.other",
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
                                description = TagDescriptions.describe(
                                    tag = tag,
                                    userNotes = tagNotes,
                                ),
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
                                onMenu = { point -> onTagMenu(tag, point) },
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

private enum class TagSection {
    ALL,
    APP,
    OTHER,
}

// Group rows and tag rows share one list; tags never start with this.
private const val GROUP_KEY_PREFIX = "\u0000group:"
private val GROUP_INDENT = 18.dp

/** A user tag group: fold, show or hide all its tags at once. Right-click to rename or delete it. */
@Composable
private fun TagGroupRow(
    name: String,
    count: Int,
    visibility: ToggleableState,
    isExpanded: Boolean,
    colors: LogColors,
    onToggleExpanded: () -> Unit,
    onVisibleChange: (Boolean) -> Unit,
    onShowOnly: () -> Unit,
    onMenu: (Point) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .then(rememberContextClickModifier(onClick = onMenu))
            .hoverable(interactionSource = interactionSource)
            .background(color = if (isHovered) colors.hover else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleExpanded,
            )
            .padding(
                start = 2.dp,
                end = 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Icon(
            key = if (isExpanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
            contentDescription = BetterLogcatBundle.message(if (isExpanded) "tags.group.collapse" else "tags.group.expand"),
        )
        TriStateCheckbox(
            state = visibility,
            onClick = { onVisibleChange(visibility != ToggleableState.On) },
        )
        Icon(
            key = AllIconsKeys.Nodes.Folder,
            contentDescription = null,
        )
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            color = if (visibility == ToggleableState.Off) colors.muted else JewelTheme.globalColors.text.normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isHovered) {
            Link(
                text = BetterLogcatBundle.message("tags.only"),
                onClick = onShowOnly,
            )
        }
        Text(
            text = count.toString(),
            color = colors.muted,
        )
    }
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
    description: String?,
    isVisible: Boolean,
    colors: LogColors,
    indent: Dp = 0.dp,
    onVisibleChange: (Boolean) -> Unit,
    onShowOnly: () -> Unit,
    onMenu: (Point) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .then(rememberContextClickModifier(onClick = onMenu))
            .hoverable(interactionSource = interactionSource)
            .background(color = if (isHovered) colors.hover else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onVisibleChange(!isVisible) },
            )
            .padding(
                start = 8.dp + indent,
                end = 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Checkbox(
            checked = isVisible,
            onCheckedChange = onVisibleChange,
        )
        TagName(
            modifier = Modifier.weight(1f),
            tag = tag,
            description = description,
            color = if (isVisible) colors.tag(tag = tag) else colors.muted,
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

/** The tag, with its description as a tooltip when it has one. */
@Composable
private fun TagName(
    modifier: Modifier = Modifier,
    tag: String,
    description: String?,
    color: Color,
) {
    if (description == null) {
        Text(
            text = tag,
            modifier = modifier,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        Tooltip(
            tooltip = { Text(text = description) },
            modifier = modifier,
        ) {
            Text(
                text = tag,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
            tagGroups = listOf(
                TagGroup(
                    name = "System noise",
                    tags = listOf("art", "adbd"),
                ),
            ),
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
