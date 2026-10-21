package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import java.awt.Point
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.format.LogTimeFormatter
import com.melihaksoy.betterlogcat.format.StackFrame
import com.melihaksoy.betterlogcat.format.UrlShortener
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.ui.logcat.model.SelectableCell
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewOptionsUiModel
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import org.jetbrains.jewel.ui.component.Tooltip

internal val CELL_GAP = 8.dp
internal val EXPAND_SLOT = 18.dp
internal val ROW_HORIZONTAL_PADDING = 4.dp

@Composable
internal fun LogRow(
    modifier: Modifier = Modifier,
    entry: LogEntry,
    previous: LogEntry?,
    processName: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    viewOptions: ViewOptionsUiModel,
    highlighter: SearchHighlighter,
    colors: LogColors,
    textStyle: TextStyle,
    isStacked: Boolean,
    selectedCell: SelectableCell = SelectableCell.NONE,
    isSelectedAbove: Boolean = false,
    keywords: KeywordHighlighter = KeywordHighlighter.None,
    urls: UrlShortener = UrlShortener.None,
    isFullyShown: Boolean = false,
    onShowAll: () -> Unit = {},
    /** Folded JSON blocks of this message, by place (see [buildMessageText]). */
    jsonFolds: Set<Int> = emptySet(),
    onToggleJsonFold: (Int) -> Unit = {},
    /** First piece of a split big message; the list shows the other pieces as more items. Null builds the text here. */
    messageOverride: AnnotatedString? = null,
    isSelectedBelow: Boolean = false,
    rawProcessName: String = processName,
    gutter: GutterUiModel = GutterUiModel.None,
    /** Shown when the pointer rests on the tag; null shows nothing. */
    tagDescription: String? = null,
    onGutterClick: (Point) -> Unit = {},
    onCopyJson: () -> Unit = {},
    onOpenInEditor: () -> Unit = {},
    onToggleExpanded: () -> Unit,
    onOpenFrame: (StackFrame) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val background = when {
        isSelected -> colors.link.copy(alpha = SELECTION_TINT)
        isHovered -> colors.hover
        else -> Color.Transparent
    }
    val verticalPadding = if (viewOptions.compactRows) 0.dp else 3.dp
    val singleLine = !viewOptions.softWrap

    val rowModifier = modifier
        .fillMaxWidth()
        .hoverable(interactionSource = interactionSource)
        .background(color = background)
        .then(
            if (isSelected) {
                Modifier.selectionOutline(
                    color = colors.link,
                    hasTop = !isSelectedAbove,
                    hasBottom = !isSelectedBelow,
                )
            } else {
                Modifier
            },
        )
        .padding(
            horizontal = ROW_HORIZONTAL_PADDING,
            vertical = verticalPadding,
        )
    if (isStacked) {
        // Narrow pane: columns on the first line, the message below at full width.
        Column(modifier = rowModifier) {
            Row(verticalAlignment = Alignment.Top) {
                HeaderCells(
                    gutter = gutter,
                    onGutterClick = onGutterClick,
                    entry = entry,
                    previous = previous,
                    processName = processName,
                    rawProcessName = rawProcessName,
                    selectedCell = selectedCell,
                    tagDescription = tagDescription,
                    isExpanded = isExpanded,
                    viewOptions = viewOptions,
                    colors = colors,
                    textStyle = textStyle,
                    onToggleExpanded = onToggleExpanded,
                )
            }
            MessageCell(
                modifier = Modifier.padding(start = LEADING_SLOT),
                entry = entry,
                isExpandable = viewOptions.isExpandable(entry = entry),
                isExpanded = isExpanded || selectedCell == SelectableCell.MESSAGE,
                isTextSelecting = selectedCell == SelectableCell.MESSAGE,
                isSingleLine = singleLine,
                prettyJson = viewOptions.prettyJson,
                highlighter = highlighter,
                keywords = keywords,
                urls = urls,
                isFullyShown = isFullyShown,
                call = gutter.call,
                jsonFolds = jsonFolds,
                colors = colors,
                textStyle = textStyle,
                textOverride = if (selectedCell == SelectableCell.MESSAGE) null else messageOverride,
                onOpenFrame = onOpenFrame,
                onShowAll = onShowAll,
                onToggleJsonFold = onToggleJsonFold,
                onCopyJson = onCopyJson,
                onOpenInEditor = onOpenInEditor,
                onToggleExpanded = onToggleExpanded,
            )
        }
    } else {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.Top,
        ) {
            HeaderCells(
                gutter = gutter,
                onGutterClick = onGutterClick,
                entry = entry,
                previous = previous,
                processName = processName,
                rawProcessName = rawProcessName,
                selectedCell = selectedCell,
                tagDescription = tagDescription,
                isExpanded = isExpanded,
                viewOptions = viewOptions,
                colors = colors,
                textStyle = textStyle,
                onToggleExpanded = onToggleExpanded,
            )
            MessageCell(
                modifier = Modifier.weight(1f),
                entry = entry,
                isExpandable = viewOptions.isExpandable(entry = entry),
                isExpanded = isExpanded || selectedCell == SelectableCell.MESSAGE,
                isTextSelecting = selectedCell == SelectableCell.MESSAGE,
                isSingleLine = singleLine,
                prettyJson = viewOptions.prettyJson,
                highlighter = highlighter,
                keywords = keywords,
                urls = urls,
                isFullyShown = isFullyShown,
                call = gutter.call,
                jsonFolds = jsonFolds,
                colors = colors,
                textStyle = textStyle,
                textOverride = if (selectedCell == SelectableCell.MESSAGE) null else messageOverride,
                onOpenFrame = onOpenFrame,
                onShowAll = onShowAll,
                onToggleJsonFold = onToggleJsonFold,
                onCopyJson = onCopyJson,
                onOpenInEditor = onOpenInEditor,
                onToggleExpanded = onToggleExpanded,
            )
        }
    }
}

private const val SELECTION_TINT = 0.16f

/** One outline around a block of selected rows: sides on every row, top and bottom only at the block ends. */
private fun Modifier.selectionOutline(
    color: Color,
    hasTop: Boolean,
    hasBottom: Boolean,
): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val half = stroke / 2
    drawLine(color = color, start = Offset(half, 0f), end = Offset(half, size.height), strokeWidth = stroke)
    drawLine(color = color, start = Offset(size.width - half, 0f), end = Offset(size.width - half, size.height), strokeWidth = stroke)
    if (hasTop) drawLine(color = color, start = Offset(0f, half), end = Offset(size.width, half), strokeWidth = stroke)
    if (hasBottom) drawLine(color = color, start = Offset(0f, size.height - half), end = Offset(size.width, size.height - half), strokeWidth = stroke)
}

/** Expand toggle plus the visible fixed-width columns, in a RowScope. */
@Composable
private fun HeaderCells(
    gutter: GutterUiModel,
    onGutterClick: (Point) -> Unit,
    entry: LogEntry,
    previous: LogEntry?,
    processName: String,
    rawProcessName: String,
    selectedCell: SelectableCell,
    tagDescription: String?,
    isExpanded: Boolean,
    viewOptions: ViewOptionsUiModel,
    colors: LogColors,
    textStyle: TextStyle,
    onToggleExpanded: () -> Unit,
) {
    run {
        Gutter(
            gutter = gutter,
            onClick = onGutterClick,
        )
        ExpandToggle(
            isExpandable = viewOptions.isExpandable(entry = entry),
            isExpanded = isExpanded,
            onToggle = onToggleExpanded,
        )
        if (LogColumn.TIME in viewOptions.visibleColumns) {
            Cell(
                widthDp = viewOptions.width(column = LogColumn.TIME),
                text = LogTimeFormatter.format(
                    entry = entry,
                    format = viewOptions.timeFormat,
                    previous = previous,
                ),
                color = colors.muted,
                textStyle = textStyle,
                colors = colors,
                isSelecting = selectedCell == SelectableCell.TIME,
            )
        }
        if (LogColumn.PROCESS in viewOptions.visibleColumns) {
            Cell(
                widthDp = viewOptions.width(column = LogColumn.PROCESS),
                text = if (viewOptions.includeThreadId) "${entry.pid}-${entry.tid}" else entry.pid.toString(),
                color = colors.muted,
                textStyle = textStyle,
                colors = colors,
                isSelecting = selectedCell == SelectableCell.PROCESS,
            )
        }
        if (LogColumn.PACKAGE in viewOptions.visibleColumns) {
            Cell(
                widthDp = viewOptions.width(column = LogColumn.PACKAGE),
                text = when {
                    selectedCell == SelectableCell.PACKAGE -> rawProcessName
                    !viewOptions.showRepeatedPackages && previous?.pid == entry.pid -> ""
                    else -> processName
                },
                color = colors.muted,
                textStyle = textStyle,
                colors = colors,
                isSelecting = selectedCell == SelectableCell.PACKAGE,
            )
        }
        if (LogColumn.TAG in viewOptions.visibleColumns) {
            Cell(
                widthDp = viewOptions.width(column = LogColumn.TAG),
                text = if (selectedCell != SelectableCell.TAG && !viewOptions.showRepeatedTags && previous?.tag == entry.tag) "" else entry.tag,
                color = if (viewOptions.colorizeTags) colors.tag(tag = entry.tag) else JewelTheme.globalColors.text.normal,
                textStyle = textStyle,
                colors = colors,
                isSelecting = selectedCell == SelectableCell.TAG,
                tooltip = tagDescription,
            )
        }
        if (LogColumn.LEVEL in viewOptions.visibleColumns) {
            LevelBadge(
                modifier = Modifier.padding(end = CELL_GAP),
                level = entry.level,
                colors = colors,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
private fun ExpandToggle(
    modifier: Modifier = Modifier,
    isExpandable: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier = modifier.width(EXPAND_SLOT),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (isExpandable) {
            Icon(
                key = if (isExpanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
                contentDescription = BetterLogcatBundle.message(if (isExpanded) "row.collapse" else "row.expand"),
                modifier = Modifier
                    .size(14.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onToggle,
                    ),
            )
        }
    }
}

@Composable
private fun Cell(
    modifier: Modifier = Modifier,
    widthDp: Int,
    text: String,
    color: Color,
    textStyle: TextStyle,
    colors: LogColors,
    isSelecting: Boolean,
    tooltip: String? = null,
) {
    if (isSelecting) {
        SelectableText(
            modifier = modifier
                .width(widthDp.dp)
                .padding(end = CELL_GAP),
            text = AnnotatedString(text = text),
            color = color,
            textStyle = textStyle,
            colors = colors,
        )
    } else if (tooltip != null && text.isNotEmpty()) {
        Tooltip(
            tooltip = { Text(text = tooltip) },
            modifier = modifier
                .width(widthDp.dp)
                .padding(end = CELL_GAP),
        ) {
            Text(
                text = text,
                color = color,
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Text(
            text = text,
            modifier = modifier
                .width(widthDp.dp)
                .padding(end = CELL_GAP),
            color = color,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun LevelBadge(
    modifier: Modifier = Modifier,
    level: LogLevel,
    colors: LogColors,
    textStyle: TextStyle,
) {
    val color = colors.level(level = level)
    Box(
        modifier = modifier
            .width(LogColumn.LEVEL.defaultWidthDp.dp - CELL_GAP)
            .background(
                color = color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(size = 3.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = level.letter.toString(),
            color = color,
            style = textStyle,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MessageCell(
    modifier: Modifier = Modifier,
    entry: LogEntry,
    isExpandable: Boolean,
    isExpanded: Boolean,
    isTextSelecting: Boolean,
    isSingleLine: Boolean,
    prettyJson: Boolean,
    highlighter: SearchHighlighter,
    keywords: KeywordHighlighter,
    urls: UrlShortener,
    isFullyShown: Boolean,
    call: NetworkCall?,
    jsonFolds: Set<Int>,
    colors: LogColors,
    textStyle: TextStyle,
    textOverride: AnnotatedString?,
    onOpenFrame: (StackFrame) -> Unit,
    onShowAll: () -> Unit,
    onToggleJsonFold: (Int) -> Unit,
    onCopyJson: () -> Unit,
    onOpenInEditor: () -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val currentOnShowAll by rememberUpdatedState(newValue = onShowAll)
    val currentOnToggleJsonFold by rememberUpdatedState(newValue = onToggleJsonFold)
    val currentOnCopyJson by rememberUpdatedState(newValue = onCopyJson)
    val currentOnOpenInEditor by rememberUpdatedState(newValue = onOpenInEditor)
    val currentOnOpenFrame by rememberUpdatedState(newValue = onOpenFrame)
    val textColor = messageColor(
        level = entry.level,
        colors = colors,
    )
    // Selecting text shows full addresses, so a copied part has the real URL.
    val shownUrls = if (isTextSelecting) UrlShortener.None else urls
    // Only a collapsed response row shows the call (its duration), so other rows ignore call updates.
    val shownCall = call.takeIf { !isExpanded && it?.responseEntryId == entry.id }
    val text = textOverride ?: remember(entry, isExpanded, isFullyShown, prettyJson, highlighter, keywords, shownUrls, colors, textColor, shownCall, jsonFolds) {
        buildMessageText(
            entry = entry,
            isExpanded = isExpanded,
            prettyJson = prettyJson,
            isFullyShown = isFullyShown,
            textColor = textColor,
            colors = colors,
            highlighter = highlighter,
            keywords = keywords,
            urls = shownUrls,
            call = shownCall,
            jsonFolds = jsonFolds,
            labels = messageLabels(),
            onOpenFrame = { currentOnOpenFrame(it) },
            onOpenUrl = { BrowserUtil.browse(it) },
            onShowAll = { currentOnShowAll() },
            onToggleJsonFold = { currentOnToggleJsonFold(it) },
        )
    }
    Column(modifier = modifier) {
        if (isTextSelecting) {
            // Free text selection: drag to select part of the message; Cmd/Ctrl+C copies only that.
            SelectableText(
                text = text,
                color = JewelTheme.globalColors.text.normal,
                textStyle = textStyle,
                colors = colors,
            )
        } else {
            Text(
                text = text,
                style = textStyle,
                softWrap = !isSingleLine,
                overflow = if (isSingleLine) TextOverflow.Clip else TextOverflow.Visible,
            )
        }
        // Split big messages continue below this row; their sticky header carries the same actions.
        if (isExpandable && !isTextSelecting && textOverride == null) {
            MessageFooter(
                entry = entry,
                isExpanded = isExpanded,
                colors = colors,
                onCopyJson = currentOnCopyJson,
                onOpenInEditor = currentOnOpenInEditor,
                onToggleExpanded = onToggleExpanded,
            )
        }
    }
}

@Composable
internal fun messageColor(
    level: LogLevel,
    colors: LogColors,
): Color = when {
    level in colors.overriddenLevels -> colors.level(level = level)
    else -> defaultMessageColor(
        level = level,
        colors = colors,
    )
}

@Composable
private fun defaultMessageColor(
    level: LogLevel,
    colors: LogColors,
): Color = when (level) {
    LogLevel.VERBOSE, LogLevel.DEBUG -> JewelTheme.globalColors.text.normal.copy(alpha = 0.78f)
    LogLevel.INFO -> JewelTheme.globalColors.text.normal
    LogLevel.WARN, LogLevel.ERROR, LogLevel.ASSERT -> colors.level(level = level)
}

internal fun messageLabels(): MessageLabels = MessageLabels(
    showAll = { lines, size -> BetterLogcatBundle.message("row.showAll", lines, size) },
    duration = { millis -> BetterLogcatBundle.message("network.duration", millis) },
    folded = { count, isArray -> BetterLogcatBundle.message(if (isArray) "row.foldedItems" else "row.foldedKeys", count) },
)

/**
 * One more piece of a split big message, under its entry's row and lined up with the message column.
 * Keeps the row's selection tint and outline; the bottom edge only on the last piece. No collapse
 * button: the sticky header over a scrolled message has Collapse.
 */
@Composable
internal fun LogContinuationRow(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    indent: Dp,
    isSelected: Boolean,
    hasBottomOutline: Boolean,
    isSingleLine: Boolean,
    colors: LogColors,
    textStyle: TextStyle,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = if (isSelected) colors.link.copy(alpha = SELECTION_TINT) else Color.Transparent)
            .then(
                if (isSelected) {
                    Modifier.selectionOutline(
                        color = colors.link,
                        hasTop = false,
                        hasBottom = hasBottomOutline,
                    )
                } else {
                    Modifier
                },
            )
            .padding(start = ROW_HORIZONTAL_PADDING),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                start = (indent - ROW_HORIZONTAL_PADDING).coerceAtLeast(minimumValue = 0.dp),
                end = ROW_HORIZONTAL_PADDING,
            ),
            style = textStyle,
            softWrap = !isSingleLine,
            overflow = if (isSingleLine) TextOverflow.Clip else TextOverflow.Visible,
        )
    }
}

// region Previews

private val previewOptions = ViewOptionsUiModel()

private fun previewEntry(
    id: Long,
    level: LogLevel,
    tag: String,
    message: String,
) = LogEntry(
    id = id,
    epochSeconds = 1_791_540_000L,
    nanos = 123_000_000,
    pid = 14_302,
    tid = 14_330,
    level = level,
    tag = tag,
    message = message,
)

@Preview
@Composable
private fun PreviewLogRow() {
    BetterLogcatPreviewTheme {
        LogRow(
            entry = previewEntry(
                id = 1,
                level = LogLevel.INFO,
                tag = "CheckoutViewModel",
                message = "Basket loaded with 3 items, total EUR 42.90",
            ),
            previous = null,
            processName = "com.example.pokedex",
            isSelected = false,
            isExpanded = false,
            viewOptions = previewOptions,
            highlighter = SearchHighlighter.None,
            colors = rememberLogColors(),
            textStyle = JewelTheme.editorTextStyle,
            isStacked = false,
            onToggleExpanded = {},
            onOpenFrame = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLogRowStackTraceExpanded() {
    BetterLogcatPreviewTheme {
        LogRow(
            entry = previewEntry(
                id = 2,
                level = LogLevel.ERROR,
                tag = "AndroidRuntime",
                message = """
                    FATAL EXCEPTION: main
                    java.lang.IllegalStateException: Pokemon id must not be blank
                    	at com.example.pokedex.data.PokemonRepository.load(PokemonRepository.kt:58)
                    	at com.example.pokedex.ui.PokemonViewModel.refresh(PokemonViewModel.kt:31)
                """.trimIndent(),
            ),
            previous = null,
            processName = "com.example.pokedex",
            isSelected = true,
            isExpanded = true,
            viewOptions = previewOptions,
            highlighter = SearchHighlighter.None,
            colors = rememberLogColors(),
            textStyle = JewelTheme.editorTextStyle,
            isStacked = false,
            onToggleExpanded = {},
            onOpenFrame = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLogRowJsonExpanded() {
    BetterLogcatPreviewTheme {
        LogRow(
            entry = previewEntry(
                id = 3,
                level = LogLevel.DEBUG,
                tag = "OkHttp",
                message = """<-- 200 https://pokeapi.co/api/v2/pokemon/25 {"id":25,"name":"pikachu","height":4,"weight":60,"stats":{"hp":35,"speed":90},"legendary":false}""",
            ),
            previous = null,
            processName = "com.example.pokedex",
            isSelected = false,
            isExpanded = true,
            viewOptions = previewOptions,
            highlighter = SearchHighlighter(
                query = "pikachu",
                isRegex = false,
                isMatchCase = false,
            ),
            colors = rememberLogColors(),
            textStyle = JewelTheme.editorTextStyle,
            isStacked = false,
            onToggleExpanded = {},
            onOpenFrame = {},
        )
    }
}

// endregion
