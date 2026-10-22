package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.intellij.openapi.util.SystemInfo
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.LogFilterMatcher
import com.melihaksoy.betterlogcat.model.TagDescriptions
import com.melihaksoy.betterlogcat.model.TagGroup
import com.melihaksoy.betterlogcat.model.TagGroups
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.model.TagNodeUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.TagSection
import com.melihaksoy.betterlogcat.ui.logcat.model.TagSectionUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.TagSelectionUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.TagTree
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
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.component.TriStateCheckbox
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.Point
import kotlin.math.roundToInt

/**
 * Side panel to show or hide tags one by one, as a tree. Nodes sort by entry count, busiest first;
 * the user's tag groups sort in with the tags, like folders. The checkbox shows or hides a tag; a
 * click on the row selects it, Cmd/Ctrl-click and Shift-click select more. Drag the selected tags
 * onto another tag to group them, onto a group to move them there, or onto a section header or the
 * empty space below to take them out of their groups. "Show all" and "Hide all" act on the rows the
 * search leaves. A tag shows a description on hover when there is one. Right-click opens the tag or
 * group menu.
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
    /** Right-click on a tag: the tags the menu acts on (the selection, or the clicked tag alone), and where. */
    onTagMenu: (List<String>, Point) -> Unit = { _, _ -> },
    onGroupMenu: (String, Point) -> Unit = { _, _ -> },
    onIntent: (LogcatIntent) -> Unit,
) {
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString().trim()
    val sections = remember(tagCounts, tagGroups, appTags, query) {
        TagTree.build(
            tagCounts = tagCounts,
            groups = tagGroups,
            appTags = appTags,
            query = query,
        )
    }
    val shownTags = remember(sections) { sections.flatMap { it.tags } }
    val tagMatcher = remember(filter) {
        LogFilterMatcher(
            filter = filter,
            processNames = emptyMap(),
        )
    }
    val hiddenCount = tagCounts.keys.count { !tagMatcher.isTagVisible(tag = it) }
    // Folded by default: a group is there to put noise away. A search opens every group it shows.
    var expandedGroups by remember { mutableStateOf(value = emptySet<String>()) }
    // Sections are open by default. A search opens every section, like groups.
    var collapsedSections by remember { mutableStateOf(value = emptySet<TagSection>()) }
    val isSectionOpen = { section: TagSection -> section !in collapsedSections || query.isNotEmpty() }
    var selection by remember { mutableStateOf(value = TagSelectionUiModel()) }
    var dragged by remember { mutableStateOf<TagDrag?>(value = null) }
    // The tag rows on screen, top to bottom: for Shift ranges and for what a drag moves.
    val rows = sections.filter { isSectionOpen(it.section) }.flatMap { section ->
        section.nodes.flatMap { node ->
            when (node) {
                is TagNodeUiModel.Tag -> listOf(node.name)
                is TagNodeUiModel.Group -> if (node.name in expandedGroups || query.isNotEmpty()) node.children.map { it.name } else emptyList()
            }
        }
    }
    val currentRows by rememberUpdatedState(newValue = rows)
    val currentGroups by rememberUpdatedState(newValue = tagGroups)
    val currentOnIntent by rememberUpdatedState(newValue = onIntent)
    val onDrop = { tags: List<String>, target: DropTarget ->
        when (
            val action = dropAction(
                tags = tags,
                target = target,
                groups = currentGroups,
            )
        ) {
            is DropAction.JoinGroup -> currentOnIntent(
                LogcatIntent.GroupTags(
                    // Tags already in the group keep their place in it.
                    tags = tags.filter {
                        TagGroups.groupOf(
                            groups = currentGroups,
                            tag = it,
                        )?.name != action.name
                    },
                    groupName = action.name,
                ),
            )
            is DropAction.NewGroup -> {
                val name = TagGroups.nextName(groups = currentGroups) { BetterLogcatBundle.message("tags.group.defaultName", it) }
                // Open the new group, so the user sees the tags land in it.
                expandedGroups = expandedGroups + name
                currentOnIntent(
                    LogcatIntent.GroupTags(
                        tags = listOf(action.with) + tags,
                        groupName = name,
                    ),
                )
            }
            DropAction.LeaveGroup -> currentOnIntent(LogcatIntent.UngroupTags(tags = tags))
            null -> Unit
        }
    }
    val onRowClick = { tag: String, keys: PointerKeyboardModifiers ->
        selection = selection.clicked(
            tag = tag,
            rows = rows,
            isToggle = if (SystemInfo.isMac) keys.isMetaPressed else keys.isCtrlPressed,
            isRange = keys.isShiftPressed,
        )
    }
    // A right-click on a selected tag acts on the whole selection; on another tag, it selects that tag first.
    val onRowMenu = { tag: String, point: Point ->
        if (tag !in selection.tags) {
            selection = TagSelectionUiModel(
                tags = setOf(tag),
                anchor = tag,
            )
        }
        onTagMenu(
            selection.tagsFor(
                tag = tag,
                rows = rows,
            ),
            point,
        )
    }
    val dropKey = dragged?.let { drag ->
        when (
            val action = dropAction(
                tags = drag.tags,
                target = drag.target,
                groups = tagGroups,
            )
        ) {
            is DropAction.JoinGroup -> GROUP_KEY_PREFIX + action.name
            is DropAction.NewGroup -> action.with
            DropAction.LeaveGroup, null -> null
        }
    }

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
                                tags = shownTags,
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
                            tags = shownTags,
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
        if (sections.isEmpty()) {
            Text(
                text = BetterLogcatBundle.message(if (tagCounts.isEmpty()) "tags.empty" else "tags.noMatch"),
                modifier = Modifier.padding(all = 10.dp),
                color = colors.muted,
            )
        } else {
            val listState = rememberLazyListState()
            // Rows are keyed by tag, so the list keeps its top row in place when rows land above it: the
            // "Your app" section when the app starts, or a tag that got busier. A list at the top stays at
            // the top, so those rows show. Read before the new rows measure.
            val isAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 } }
            val shownSections = remember { ShownSections() }
            val keepsTop = isAtTop && shownSections.value != sections
            shownSections.value = sections
            SideEffect {
                if (keepsTop) listState.requestScrollToItem(index = 0)
            }
            // Rows keep a chevron slot only when a group shows, so a panel without groups stays as it was.
            val hasGroups = sections.any { section -> section.nodes.any { it is TagNodeUiModel.Group } }
            val slot = if (hasGroups) CHEVRON_SLOT else 0.dp
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                // Not required unconsumed: the row's click handler takes the press too.
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val buttons = currentEvent.buttons
                                val isMacContextClick = SystemInfo.isMac && currentEvent.keyboardModifiers.isCtrlPressed
                                if (!buttons.isPrimaryPressed || isMacContextClick) return@awaitEachGesture
                                if (itemAt(listState = listState, y = down.position.y) == null) {
                                    // A click on the empty space below the rows clears the selection.
                                    selection = TagSelectionUiModel()
                                    return@awaitEachGesture
                                }
                                val tag = (targetAt(listState = listState, y = down.position.y) as? DropTarget.OnTag)?.tag
                                    ?: return@awaitEachGesture
                                // Past the slop the drag owns the gesture; the consumed move cancels the row click.
                                val start = awaitTouchSlopOrCancellation(pointerId = down.id) { change, _ -> change.consume() }
                                    ?: return@awaitEachGesture
                                val tags = selection.tagsFor(
                                    tag = tag,
                                    rows = currentRows,
                                )
                                // Dragging a row outside the selection selects it, as in the IDE's trees.
                                if (tag !in selection.tags) {
                                    selection = TagSelectionUiModel(
                                        tags = setOf(tag),
                                        anchor = tag,
                                    )
                                }
                                dragged = TagDrag(
                                    tags = tags,
                                    position = start.position,
                                    target = targetAt(
                                        listState = listState,
                                        y = start.position.y,
                                    ),
                                )
                                val edge = AUTOSCROLL_EDGE.toPx()
                                val isDropped = drag(pointerId = start.id) { change ->
                                    val y = change.position.y
                                    if (y < edge) listState.dispatchRawDelta(delta = (y - edge) / AUTOSCROLL_DIVISOR)
                                    if (y > size.height - edge) listState.dispatchRawDelta(delta = (y - size.height + edge) / AUTOSCROLL_DIVISOR)
                                    dragged = TagDrag(
                                        tags = tags,
                                        position = change.position,
                                        target = targetAt(
                                            listState = listState,
                                            y = y,
                                        ),
                                    )
                                    change.consume()
                                }
                                val last = dragged
                                dragged = null
                                if (isDropped && last != null) onDrop(last.tags, last.target)
                            }
                        },
                    state = listState,
                ) {
                    sections.forEach { section ->
                        val isOpen = isSectionOpen(section.section)
                        if (section.section != TagSection.ALL) {
                            item(key = HEADER_KEY_PREFIX + section.section.name) {
                                GroupHeaderRow(
                                    title = BetterLogcatBundle.message(
                                        if (section.section == TagSection.APP) "tags.group.app" else "tags.group.other",
                                        section.tags.size,
                                    ),
                                    isExpanded = isOpen,
                                    colors = colors,
                                    onToggleExpanded = {
                                        collapsedSections = if (section.section in collapsedSections) {
                                            collapsedSections - section.section
                                        } else {
                                            collapsedSections + section.section
                                        }
                                    },
                                    onShowAll = {
                                        onIntent(
                                            LogcatIntent.SetTagsVisible(
                                                tags = section.tags,
                                                isVisible = true,
                                            ),
                                        )
                                    },
                                    onHideAll = {
                                        onIntent(
                                            LogcatIntent.SetTagsVisible(
                                                tags = section.tags,
                                                isVisible = false,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                        if (!isOpen) return@forEach
                        section.nodes.forEach { node ->
                            when (node) {
                                is TagNodeUiModel.Tag -> item(key = node.name) {
                                    TagRow(
                                        tag = node.name,
                                        count = node.count,
                                        rates = tagRates[node.name],
                                        description = TagDescriptions.describe(
                                            tag = node.name,
                                            userNotes = tagNotes,
                                        ),
                                        isVisible = tagMatcher.isTagVisible(tag = node.name),
                                        isSelected = node.name in selection.tags,
                                        isDragged = dragged?.tags?.contains(node.name) == true,
                                        isDropTarget = dropKey == node.name,
                                        slot = slot,
                                        colors = colors,
                                        onClick = { keys -> onRowClick(node.name, keys) },
                                        onVisibleChange = { isVisible ->
                                            onIntent(
                                                LogcatIntent.SetTagsVisible(
                                                    tags = listOf(node.name),
                                                    isVisible = isVisible,
                                                ),
                                            )
                                        },
                                        onShowOnly = { onIntent(LogcatIntent.ShowOnlyTag(tag = node.name)) },
                                        onMenu = { point -> onRowMenu(node.name, point) },
                                    )
                                }
                                is TagNodeUiModel.Group -> {
                                    val isExpanded = node.name in expandedGroups || query.isNotEmpty()
                                    val visibleCount = node.group.tags.count { tagMatcher.isTagVisible(tag = it) }
                                    item(key = GROUP_KEY_PREFIX + node.name) {
                                        TagGroupRow(
                                            name = node.name,
                                            count = node.count,
                                            visibility = when (visibleCount) {
                                                node.group.tags.size -> ToggleableState.On
                                                0 -> ToggleableState.Off
                                                else -> ToggleableState.Indeterminate
                                            },
                                            isExpanded = isExpanded,
                                            isDropTarget = dropKey == GROUP_KEY_PREFIX + node.name,
                                            colors = colors,
                                            onToggleExpanded = {
                                                expandedGroups = if (node.name in expandedGroups) expandedGroups - node.name else expandedGroups + node.name
                                            },
                                            onVisibleChange = { isVisible ->
                                                onIntent(
                                                    LogcatIntent.SetTagsVisible(
                                                        tags = node.group.tags,
                                                        isVisible = isVisible,
                                                    ),
                                                )
                                            },
                                            onShowOnly = { onIntent(LogcatIntent.ShowOnlyTags(tags = node.group.tags)) },
                                            onMenu = { point -> onGroupMenu(node.name, point) },
                                        )
                                    }
                                    if (isExpanded) {
                                        items(
                                            items = node.children,
                                            key = { it.name },
                                        ) { child ->
                                            TagRow(
                                                tag = child.name,
                                                count = child.count,
                                                rates = tagRates[child.name],
                                                description = TagDescriptions.describe(
                                                    tag = child.name,
                                                    userNotes = tagNotes,
                                                ),
                                                isVisible = tagMatcher.isTagVisible(tag = child.name),
                                                isSelected = child.name in selection.tags,
                                                isDragged = dragged?.tags?.contains(child.name) == true,
                                                isDropTarget = false,
                                                slot = slot,
                                                indent = GROUP_INDENT,
                                                colors = colors,
                                                onClick = { keys -> onRowClick(child.name, keys) },
                                                onVisibleChange = { isVisible ->
                                                    onIntent(
                                                        LogcatIntent.SetTagsVisible(
                                                            tags = listOf(child.name),
                                                            isVisible = isVisible,
                                                        ),
                                                    )
                                                },
                                                onShowOnly = { onIntent(LogcatIntent.ShowOnlyTag(tag = child.name)) },
                                                onMenu = { point -> onRowMenu(child.name, point) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    scrollState = listState,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
                dragged?.let { drag ->
                    DragLabel(
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (drag.position.x + DRAG_LABEL_OFFSET.toPx()).roundToInt(),
                                y = (drag.position.y + DRAG_LABEL_OFFSET.toPx()).roundToInt(),
                            )
                        },
                        tags = drag.tags,
                        colors = colors,
                    )
                }
            }
        }
    }
}

/** The sections the list last composed. Plain, not state: a change must not compose again. */
private class ShownSections {
    var value: List<TagSectionUiModel>? = null
}

/** Tags on the move: where the pointer is and what is under it. */
private data class TagDrag(
    val tags: List<String>,
    val position: Offset,
    val target: DropTarget,
)

/** What is under the pointer while a tag is dragged. */
private sealed interface DropTarget {
    data class OnTag(val tag: String) : DropTarget
    data class OnGroup(val name: String) : DropTarget

    /** A section header, or the empty space below the rows. */
    data object Outside : DropTarget
}

/** What a drop does. */
private sealed interface DropAction {
    data class JoinGroup(val name: String) : DropAction

    /** A new group with the dragged tags and [with], the tag they were dropped on. */
    data class NewGroup(val with: String) : DropAction
    data object LeaveGroup : DropAction
}

/** Null when the drop changes nothing, for example tags dropped on one of them or on their own group. */
private fun dropAction(
    tags: List<String>,
    target: DropTarget,
    groups: List<TagGroup>,
): DropAction? {
    val ownGroups = tags.map { tag ->
        TagGroups.groupOf(
            groups = groups,
            tag = tag,
        )?.name
    }.toSet()
    val join = { name: String -> if (ownGroups == setOf(name)) null else DropAction.JoinGroup(name = name) }
    return when (target) {
        is DropTarget.OnTag -> {
            val targetGroup = TagGroups.groupOf(
                groups = groups,
                tag = target.tag,
            )?.name
            when {
                target.tag in tags -> null
                targetGroup == null -> DropAction.NewGroup(with = target.tag)
                else -> join(targetGroup)
            }
        }
        is DropTarget.OnGroup -> join(target.name)
        DropTarget.Outside -> if (ownGroups == setOf(null)) null else DropAction.LeaveGroup
    }
}

private fun itemAt(
    listState: LazyListState,
    y: Float,
): LazyListItemInfo? = listState.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size }

private fun targetAt(
    listState: LazyListState,
    y: Float,
): DropTarget {
    val item = itemAt(
        listState = listState,
        y = y,
    )
    val key = item?.key as? String ?: return DropTarget.Outside
    return when {
        key.startsWith(prefix = GROUP_KEY_PREFIX) -> DropTarget.OnGroup(name = key.removePrefix(prefix = GROUP_KEY_PREFIX))
        key.startsWith(prefix = HEADER_KEY_PREFIX) -> DropTarget.Outside
        else -> DropTarget.OnTag(tag = key)
    }
}

// Group, header and tag rows share one list; tags never start with these.
private const val GROUP_KEY_PREFIX = "\u0000group:"
private const val HEADER_KEY_PREFIX = "\u0000header:"

// A child's chevron slot sits under its group's checkbox, as in the IDE's project tree.
private val CHEVRON_SLOT = 16.dp
private val ROW_GAP = 6.dp
private val GROUP_INDENT = CHEVRON_SLOT + ROW_GAP
private val ROW_START = 8.dp
private val AUTOSCROLL_EDGE = 24.dp
private const val AUTOSCROLL_DIVISOR = 3f
private val DRAG_LABEL_OFFSET = 12.dp
private const val DRAGGED_ALPHA = 0.4f
private const val DROP_TINT = 0.22f

/** A user tag group, as a folder: fold, show or hide all its tags at once. Right-click to rename or delete it. */
@Composable
private fun TagGroupRow(
    name: String,
    count: Int,
    visibility: ToggleableState,
    isExpanded: Boolean,
    isDropTarget: Boolean,
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
            .background(
                color = rowBackground(
                    isDropTarget = isDropTarget,
                    isHovered = isHovered,
                    colors = colors,
                ),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleExpanded,
            )
            .padding(
                start = ROW_START,
                end = 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = ROW_GAP),
    ) {
        Icon(
            key = if (isExpanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
            contentDescription = BetterLogcatBundle.message(if (isExpanded) "tags.group.collapse" else "tags.group.expand"),
            modifier = Modifier.width(CHEVRON_SLOT),
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

/** "Your app" or "Other": a click folds the section. The links show or hide all its tags. */
@Composable
private fun GroupHeaderRow(
    title: String,
    isExpanded: Boolean,
    colors: LogColors,
    onToggleExpanded: () -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colors.headerBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleExpanded,
            )
            .padding(
                start = ROW_START,
                end = 10.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = ROW_GAP),
    ) {
        Icon(
            key = if (isExpanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
            contentDescription = BetterLogcatBundle.message(if (isExpanded) "tags.group.collapse" else "tags.group.expand"),
            modifier = Modifier.width(CHEVRON_SLOT),
        )
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
    isSelected: Boolean,
    isDragged: Boolean,
    isDropTarget: Boolean,
    slot: Dp,
    colors: LogColors,
    indent: Dp = 0.dp,
    onClick: (PointerKeyboardModifiers) -> Unit,
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
            .alpha(alpha = if (isDragged) DRAGGED_ALPHA else 1f)
            .then(rememberContextClickModifier(onClick = onMenu))
            .hoverable(interactionSource = interactionSource)
            .background(
                color = rowBackground(
                    isDropTarget = isDropTarget,
                    isSelected = isSelected,
                    isHovered = isHovered,
                    colors = colors,
                ),
            )
            .then(rememberRowClickModifier(onClick = onClick))
            .padding(
                start = ROW_START + indent,
                end = 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = ROW_GAP),
    ) {
        if (slot > 0.dp) Spacer(modifier = Modifier.width(slot))
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

/**
 * A primary click on the row, with the keys held at the press. Presses the checkbox or the "only"
 * link take are not row clicks, and a drag cancels the click.
 */
@Composable
private fun rememberRowClickModifier(onClick: (PointerKeyboardModifiers) -> Unit): Modifier {
    val currentOnClick by rememberUpdatedState(newValue = onClick)
    return Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown()
            if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            val keys = currentEvent.keyboardModifiers
            waitForUpOrCancellation() ?: return@awaitEachGesture
            currentOnClick(keys)
        }
    }
}

private fun rowBackground(
    isDropTarget: Boolean,
    isSelected: Boolean = false,
    isHovered: Boolean,
    colors: LogColors,
): Color = when {
    isDropTarget -> colors.link.copy(alpha = DROP_TINT)
    isSelected -> colors.selection
    isHovered -> colors.hover
    else -> Color.Transparent
}

/** The tag name under the pointer while it is dragged, or the count of tags when there are more. */
@Composable
private fun DragLabel(
    modifier: Modifier = Modifier,
    tags: List<String>,
    colors: LogColors,
) {
    val shape = RoundedCornerShape(size = 4.dp)
    val tag = tags.singleOrNull()
    Text(
        text = tag ?: BetterLogcatBundle.message("tags.drag.count", tags.size),
        modifier = modifier
            .background(
                color = JewelTheme.globalColors.panelBackground,
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = colors.link,
                shape = shape,
            )
            .padding(
                horizontal = 6.dp,
                vertical = 2.dp,
            ),
        color = tag?.let { colors.tag(tag = it) } ?: JewelTheme.globalColors.text.normal,
        maxLines = 1,
    )
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
            appTags = setOf("OkHttp", "PokedexApp", "AndroidRuntime"),
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
