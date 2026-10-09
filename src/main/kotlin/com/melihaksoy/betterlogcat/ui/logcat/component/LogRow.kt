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
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
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
    isFullyShown: Boolean = false,
    onShowAll: () -> Unit = {},
    /** First piece of a split big message; the list shows the other pieces as more items. Null builds the text here. */
    messageOverride: AnnotatedString? = null,
    isSelectedBelow: Boolean = false,
    rawProcessName: String = processName,
    gutter: GutterUiModel = GutterUiModel.None,
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
                isExpanded = isExpanded || selectedCell == SelectableCell.MESSAGE,
                isTextSelecting = selectedCell == SelectableCell.MESSAGE,
                isSingleLine = singleLine,
                prettyJson = viewOptions.prettyJson,
                highlighter = highlighter,
                keywords = keywords,
                isFullyShown = isFullyShown,
                colors = colors,
                textStyle = textStyle,
                textOverride = if (selectedCell == SelectableCell.MESSAGE) null else messageOverride,
                onOpenFrame = onOpenFrame,
                onShowAll = onShowAll,
                onCopyJson = onCopyJson,
                onOpenInEditor = onOpenInEditor,
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
                isExpanded = isExpanded,
                viewOptions = viewOptions,
                colors = colors,
                textStyle = textStyle,
                onToggleExpanded = onToggleExpanded,
            )
            MessageCell(
                modifier = Modifier.weight(1f),
                entry = entry,
                isExpanded = isExpanded || selectedCell == SelectableCell.MESSAGE,
                isTextSelecting = selectedCell == SelectableCell.MESSAGE,
                isSingleLine = singleLine,
                prettyJson = viewOptions.prettyJson,
                highlighter = highlighter,
                keywords = keywords,
                isFullyShown = isFullyShown,
                colors = colors,
                textStyle = textStyle,
                textOverride = if (selectedCell == SelectableCell.MESSAGE) null else messageOverride,
                onOpenFrame = onOpenFrame,
                onShowAll = onShowAll,
                onCopyJson = onCopyJson,
                onOpenInEditor = onOpenInEditor,
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
            isExpandable = entry.isExpandable,
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
    isExpanded: Boolean,
    isTextSelecting: Boolean,
    isSingleLine: Boolean,
    prettyJson: Boolean,
    highlighter: SearchHighlighter,
    keywords: KeywordHighlighter,
    isFullyShown: Boolean,
    colors: LogColors,
    textStyle: TextStyle,
    textOverride: AnnotatedString?,
    onOpenFrame: (StackFrame) -> Unit,
    onShowAll: () -> Unit,
    onCopyJson: () -> Unit,
    onOpenInEditor: () -> Unit,
) {
    val currentOnShowAll by rememberUpdatedState(newValue = onShowAll)
    val currentOnCopyJson by rememberUpdatedState(newValue = onCopyJson)
    val currentOnOpenInEditor by rememberUpdatedState(newValue = onOpenInEditor)
    val currentOnOpenFrame by rememberUpdatedState(newValue = onOpenFrame)
    val textColor = messageColor(
        level = entry.level,
        colors = colors,
    )
    val text = textOverride ?: remember(entry, isExpanded, isFullyShown, prettyJson, highlighter, keywords, colors, textColor) {
        buildMessageText(
            entry = entry,
            isExpanded = isExpanded,
            prettyJson = prettyJson,
            isFullyShown = isFullyShown,
            textColor = textColor,
            colors = colors,
            highlighter = highlighter,
            keywords = keywords,
            labels = messageLabels(),
            onOpenFrame = { currentOnOpenFrame(it) },
            onOpenUrl = { BrowserUtil.browse(it) },
            onShowAll = { currentOnShowAll() },
            onCopyJson = { currentOnCopyJson() },
            onOpenInEditor = { currentOnOpenInEditor() },
        )
    }
    if (isTextSelecting) {
        // Free text selection: drag to select part of the message; Cmd/Ctrl+C copies only that.
        SelectableText(
            modifier = modifier,
            text = text,
            color = JewelTheme.globalColors.text.normal,
            textStyle = textStyle,
            colors = colors,
        )
    } else {
        Text(
            text = text,
            modifier = modifier,
            style = textStyle,
            softWrap = !isSingleLine,
            overflow = if (isSingleLine) TextOverflow.Clip else TextOverflow.Visible,
        )
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
    moreLines = { count -> BetterLogcatBundle.message("row.moreLines", count) },
    json = { size -> BetterLogcatBundle.message("row.json", size) },
    more = { size -> BetterLogcatBundle.message("row.more", size) },
    showAll = { lines, size -> BetterLogcatBundle.message("row.showAll", lines, size) },
    copyJson = BetterLogcatBundle.message("row.copyJson"),
    openInEditor = BetterLogcatBundle.message("row.open"),
)

/**
 * One more piece of a split big message, under its entry's row and lined up with the message column.
 * Keeps the row's selection tint and outline; the bottom edge only on the last piece.
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
    onCollapse: () -> Unit,
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
        // The gutter keeps a collapse button on every piece, so a huge message folds from anywhere.
        Box(
            modifier = Modifier.width(LEADING_SLOT),
            contentAlignment = Alignment.TopCenter,
        ) {
            Icon(
                key = AllIconsKeys.General.ChevronUp,
                contentDescription = BetterLogcatBundle.message("row.collapse"),
                modifier = Modifier
                    .size(14.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onCollapse,
                    ),
            )
        }
        Text(
            text = text,
            modifier = Modifier.padding(
                start = (indent - ROW_HORIZONTAL_PADDING - LEADING_SLOT).coerceAtLeast(minimumValue = 0.dp),
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
            processName = "com.vintagegarage.app",
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
                    java.lang.IllegalStateException: Listing id must not be blank
                    	at com.vintagegarage.listing.ListingRepository.load(ListingRepository.kt:58)
                    	at com.vintagegarage.listing.ListingViewModel.refresh(ListingViewModel.kt:31)
                """.trimIndent(),
            ),
            previous = null,
            processName = "com.vintagegarage.app",
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
                message = """<-- 200 https://api.vintagegarage.com/v2/listings/88213 {"id":88213,"make":"Jaguar","model":"E-Type","year":1967,"price":{"amount":98500,"currency":"GBP"},"sold":false}""",
            ),
            previous = null,
            processName = "com.vintagegarage.app",
            isSelected = false,
            isExpanded = true,
            viewOptions = previewOptions,
            highlighter = SearchHighlighter(
                query = "Jaguar",
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
