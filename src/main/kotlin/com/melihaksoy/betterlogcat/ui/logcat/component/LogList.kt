package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.format.StackFrame
import com.intellij.ide.BrowserUtil
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed as isPointerCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed as isPointerMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed as isPointerShiftPressed
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.ide.CopyProvider
import com.intellij.openapi.util.SystemInfo
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.format.CopyMode
import com.melihaksoy.betterlogcat.ide.PackageAliasStore
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.ui.logcat.model.SelectableCell
import com.melihaksoy.betterlogcat.ui.logcat.model.SelectionUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.TextSelectionUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewOptionsUiModel
import com.melihaksoy.betterlogcat.ui.logcat.preview.LogcatPreviewData
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.actionSystem.provideData
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import java.awt.Point
import kotlin.math.roundToInt

private const val DOUBLE_CLICK_MILLIS = 400L
private val MENU_OFFSET = Offset(x = 4f, y = 4f)
private val MIN_MESSAGE_WIDTH = 260.dp
private const val PAGE_ROWS = 20

/**
 * The log table. Mouse: click selects, Shift extends, Cmd/Ctrl toggles, drag selects a block,
 * double click expands. Keys: arrows move, Shift+arrows extend, Right/Left expand/collapse,
 * Cmd/Ctrl+C copies, Cmd/Ctrl+A selects all, Esc clears.
 */
@Composable
internal fun LogList(
    modifier: Modifier = Modifier,
    state: LogcatState,
    colors: LogColors,
    onIntent: (LogcatIntent) -> Unit,
    onRowMenu: (LogEntry, Point) -> Unit,
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val currentState by rememberUpdatedState(newValue = state)
    val currentOnIntent by rememberUpdatedState(newValue = onIntent)
    val currentOnRowMenu by rememberUpdatedState(newValue = onRowMenu)
    var listCoordinates by remember { mutableStateOf<LayoutCoordinates?>(value = null) }
    // The cell in text selection mode (double click on time, PID, package, tag or message).
    var textSelection by remember { mutableStateOf(value = TextSelectionUiModel()) }
    val currentTextSelection by rememberUpdatedState(newValue = textSelection)
    // Expanded messages past the size cap that the user opened in full with "Show all".
    var fullyShownIds by remember { mutableStateOf(value = emptySet<Long>()) }
    var liveWidths by remember(state.viewOptions.columnWidths) { mutableStateOf(value = state.viewOptions.columnWidths) }
    val viewOptions = state.viewOptions.copy(columnWidths = liveWidths)
    val highlighter = remember(state.filter.query, state.filter.isRegex, state.filter.isMatchCase) {
        SearchHighlighter.from(filter = state.filter)
    }
    val textStyle = JewelTheme.editorTextStyle
    val keywords = remember(state.highlightRules) { KeywordHighlighter(rules = state.highlightRules) }
    val labels = remember { messageLabels() }
    val levelTextColors = LogLevel.entries.associateWith {
        messageColor(
            level = it,
            colors = colors,
        )
    }
    val pieceCache = remember { PieceCache() }
    val layout = buildLayout(
        state = state,
        textSelection = textSelection,
        fullyShownIds = fullyShownIds,
        highlighter = highlighter,
        keywords = keywords,
        colors = colors,
        levelTextColors = levelTextColors,
        labels = labels,
        cache = pieceCache,
        onOpenFrame = { currentOnIntent(LogcatIntent.OpenFrame(frame = it)) },
        onShowAll = { id -> fullyShownIds = fullyShownIds + id },
    )
    val currentLayout by rememberUpdatedState(newValue = layout)
    val copyProvider = remember {
        object : CopyProvider {
            override fun performCopy(dataContext: DataContext) = currentOnIntent(LogcatIntent.Copy(mode = currentState.viewOptions.defaultCopyMode))

            // Off during text selection, so Cmd/Ctrl+C reaches the selected text instead of copying rows.
            override fun isCopyEnabled(dataContext: DataContext): Boolean =
                !currentTextSelection.isActive && currentState.selection.selectedIds.isNotEmpty()

            override fun isCopyVisible(dataContext: DataContext): Boolean = true

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }
    }

    val isFollowing = rememberFollowMode(
        listState = listState,
        itemCount = layout.itemCount,
        selectionLeadId = state.selection.leadId,
    )
    ScrollToLead(
        listState = listState,
        state = state,
        layout = layout,
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columnsWidth = EXPAND_SLOT + ROW_HORIZONTAL_PADDING * 2 +
            viewOptions.visibleColumns.sumOf { viewOptions.width(column = it) }.dp
        val isStacked = maxWidth < columnsWidth + MIN_MESSAGE_WIDTH
        val currentIsStacked by rememberUpdatedState(newValue = isStacked)
        val messageIndent = EXPAND_SLOT + ROW_HORIZONTAL_PADDING + viewOptions.visibleColumns.sumOf {
            if (it == LogColumn.LEVEL) it.defaultWidthDp else viewOptions.width(column = it)
        }.dp
        val messageIndentStacked = EXPAND_SLOT + ROW_HORIZONTAL_PADDING
        Column(modifier = Modifier.fillMaxSize()) {
            LogHeader(
                viewOptions = viewOptions,
                colors = colors,
                onResize = { column, width -> liveWidths = liveWidths + (column to width) },
                onResizeEnd = { column ->
                    onIntent(
                        LogcatIntent.ResizeColumn(
                            column = column,
                            widthDp = liveWidths[column] ?: column.defaultWidthDp,
                        ),
                    )
                },
            )
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { listCoordinates = it }
                        .provideData { set(PlatformDataKeys.COPY_PROVIDER.name, copyProvider) }
                        .focusRequester(focusRequester = focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            handleKey(
                                event = event,
                                state = currentState,
                                isTextSelecting = currentTextSelection.isActive,
                                onExitTextSelection = { textSelection = TextSelectionUiModel() },
                                onContextMenu = {
                                    val leadIndex = currentState.entries.binarySearch { it.id.compareTo(other = currentState.selection.leadId) }
                                    val lead = currentState.entries.getOrNull(index = leadIndex)
                                    val leadItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentLayout.firstItemOf(entryIndex = leadIndex) }
                                    if (lead != null && leadItem != null) {
                                        currentOnRowMenu(
                                            lead,
                                            toSwingPoint(
                                                coordinates = listCoordinates,
                                                local = Offset(x = KEY_MENU_X, y = leadItem.offset.toFloat()) + MENU_OFFSET,
                                                density = density,
                                            ),
                                        )
                                    }
                                },
                                onIntent = currentOnIntent,
                            )
                        }
                        .pointerInput(Unit) {
                            val clicks = ClickTracker()
                            awaitEachGesture {
                                // Any button press: left, right (secondary) or Ctrl+click. Hover moves end the gesture at once.
                                val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                if (event.type != PointerEventType.Press) return@awaitEachGesture
                                val down = event.changes.firstOrNull() ?: return@awaitEachGesture
                                if (down.isConsumed) return@awaitEachGesture
                                val entries = currentState.entries
                                val index = indexAt(
                                    listState = listState,
                                    y = down.position.y,
                                )
                                val ref = currentLayout.itemAt(item = index)
                                val entry = entries.getOrNull(index = ref.entryIndex) ?: return@awaitEachGesture
                                // The row in text selection mode belongs to its text box. Taking focus here
                                // would clear the selection, so leave its clicks and drags alone.
                                if (currentTextSelection.isActive && entry.id == currentTextSelection.entryId) return@awaitEachGesture
                                focusRequester.requestFocus()

                                // Secondary button, or Ctrl+click on macOS.
                                val isContextClick = event.buttons.isSecondaryPressed ||
                                    event.button == PointerButton.Secondary ||
                                    (SystemInfo.isMac && event.keyboardModifiers.isPointerCtrlPressed)
                                if (isContextClick) {
                                    if (entry.id !in currentState.selection.selectedIds) {
                                        currentOnIntent(
                                            LogcatIntent.PressRow(
                                                id = entry.id,
                                                isExtend = false,
                                                isToggle = false,
                                            ),
                                        )
                                    }
                                    down.consume()
                                    // Open on release, a little off the cursor: opened on press, the release landed on the
                                    // first menu item and closed the menu at once.
                                    do {
                                        val release = awaitPointerEvent()
                                    } while (release.changes.any { it.pressed })
                                    currentOnRowMenu(
                                        entry,
                                        toSwingPoint(
                                            coordinates = listCoordinates,
                                            local = down.position + MENU_OFFSET,
                                            density = density,
                                        ),
                                    )
                                    return@awaitEachGesture
                                }

                                if (entry.id != currentTextSelection.entryId) textSelection = TextSelectionUiModel()
                                val modifiers = event.keyboardModifiers
                                val isToggle = if (SystemInfo.isMac) modifiers.isPointerMetaPressed else modifiers.isPointerCtrlPressed
                                val isExtend = modifiers.isPointerShiftPressed
                                currentOnIntent(
                                    LogcatIntent.PressRow(
                                        id = entry.id,
                                        isExtend = isExtend,
                                        isToggle = isToggle,
                                    ),
                                )
                                if (isToggle || isExtend) return@awaitEachGesture
                                if (clicks.isDoubleClick(id = entry.id, timeMillis = down.uptimeMillis)) {
                                    val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                    textSelection = TextSelectionUiModel(
                                        entryId = entry.id,
                                        cell = if (ref.chunk > 0) SelectableCell.MESSAGE else cellAt(
                                            x = down.position.x,
                                            yInRow = down.position.y - (item?.offset ?: 0).toFloat(),
                                            viewOptions = currentState.viewOptions,
                                            isStacked = currentIsStacked,
                                            density = this,
                                        ),
                                    )
                                    return@awaitEachGesture
                                }

                                var lastIndex = index
                                while (true) {
                                    val move = awaitPointerEvent()
                                    val change = move.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val y = change.position.y
                                    val height = size.height
                                    if (y < 0f) listState.dispatchRawDelta(delta = y / 3f)
                                    if (y > height) listState.dispatchRawDelta(delta = (y - height) / 3f)
                                    val currentIndex = indexAt(
                                        listState = listState,
                                        y = y.coerceIn(minimumValue = 0f, maximumValue = height.toFloat()),
                                    )
                                    val current = currentState.entries.getOrNull(index = currentLayout.itemAt(item = currentIndex).entryIndex) ?: continue
                                    if (currentIndex != lastIndex) {
                                        lastIndex = currentIndex
                                        currentOnIntent(
                                            LogcatIntent.DragSelect(
                                                anchorId = entry.id,
                                                currentId = current.id,
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                    state = listState,
                ) {
                    items(
                        count = layout.itemCount,
                        key = { item ->
                            val ref = layout.itemAt(item = item)
                            val id = layout.entries[ref.entryIndex].id
                            if (ref.chunk == 0) id else "$id:${ref.chunk}"
                        },
                        contentType = { item -> if (layout.itemAt(item = item).chunk == 0) 0 else 1 },
                    ) { item ->
                        val ref = layout.itemAt(item = item)
                        val index = ref.entryIndex
                        val entry = state.entries[index]
                        val isSelected = entry.id in state.selection.selectedIds
                        val isNextSelected = state.entries.getOrNull(index = index + 1)?.id in state.selection.selectedIds
                        val pieces = layout.chunked[index]
                        if (ref.chunk == 0) {
                            LogRow(
                                entry = entry,
                                previous = state.entries.getOrNull(index = index - 1),
                                processName = PackageAliasStore.display(
                                    processName = state.processNames[entry.pid].orEmpty(),
                                    aliases = state.packageAliases,
                                ),
                                isSelected = isSelected,
                                keywords = keywords,
                                isFullyShown = entry.id in fullyShownIds,
                                onShowAll = { fullyShownIds = fullyShownIds + entry.id },
                                messageOverride = pieces?.first(),
                                isSelectedAbove = state.entries.getOrNull(index = index - 1)?.id in state.selection.selectedIds,
                                // A split message continues below, so the outline does too.
                                isSelectedBelow = isNextSelected || pieces != null,
                                isExpanded = state.isExpanded(entry = entry),
                                viewOptions = viewOptions,
                                highlighter = highlighter,
                                colors = colors,
                                textStyle = textStyle,
                                isStacked = isStacked,
                                selectedCell = textSelection.cellFor(id = entry.id),
                                rawProcessName = state.processNames[entry.pid].orEmpty(),
                                onToggleExpanded = { onIntent(LogcatIntent.ToggleExpanded(id = entry.id)) },
                                onOpenFrame = { onIntent(LogcatIntent.OpenFrame(frame = it)) },
                            )
                        } else if (pieces != null) {
                            LogContinuationRow(
                                text = pieces[ref.chunk],
                                indent = if (isStacked) messageIndentStacked else messageIndent,
                                isSelected = isSelected,
                                hasBottomOutline = ref.chunk == pieces.lastIndex && !isNextSelected,
                                isSingleLine = !viewOptions.softWrap,
                                colors = colors,
                                textStyle = textStyle,
                            )
                        }
                    }
                }
                if (textSelection.isActive) {
                    Text(
                        text = BetterLogcatBundle.message("list.textSelectionHint"),
                        modifier = Modifier
                            .align(alignment = Alignment.TopEnd)
                            .padding(
                                top = 6.dp,
                                end = 20.dp,
                            )
                            .background(
                                color = JewelTheme.globalColors.panelBackground,
                                shape = RoundedCornerShape(size = 4.dp),
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 3.dp,
                            ),
                        color = colors.muted,
                    )
                }
                VerticalScrollbar(
                    scrollState = listState,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
                if (!isFollowing && state.entries.isNotEmpty()) {
                    JumpToLatestButton(
                        modifier = Modifier
                            .align(alignment = Alignment.BottomEnd)
                            .padding(
                                end = 20.dp,
                                bottom = 12.dp,
                            ),
                        listState = listState,
                        lastIndex = layout.itemCount - 1,
                    )
                }
            }
        }
    }
}

/**
 * Follows new rows while the list sits at the bottom. Scrolling up or selecting a row stops it
 * ([selectionLeadId] changes); scrolling back to the bottom, or the jump button, starts it again.
 */
@Composable
private fun rememberFollowMode(
    listState: LazyListState,
    itemCount: Int,
    selectionLeadId: Long,
): Boolean {
    var isFollowing by remember { mutableStateOf(value = true) }
    LaunchedEffect(selectionLeadId) {
        if (selectionLeadId != SelectionUiModel.NO_ROW) isFollowing = false
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.canScrollForward }
            .collect { (isScrolling, canScrollForward) ->
                if (isScrolling && canScrollForward) isFollowing = false
                if (!canScrollForward) isFollowing = true
            }
    }
    LaunchedEffect(itemCount, isFollowing) {
        if (isFollowing && itemCount > 0) listState.scrollToItem(index = itemCount - 1)
    }
    return isFollowing
}

/** Keeps the keyboard lead row on screen. */
@Composable
private fun ScrollToLead(
    listState: LazyListState,
    state: LogcatState,
    layout: LogListLayout,
) {
    val leadId = state.selection.leadId
    LaunchedEffect(leadId) {
        val entryIndex = state.entries.binarySearch { it.id.compareTo(other = leadId) }
        if (entryIndex < 0) return@LaunchedEffect
        val index = layout.firstItemOf(entryIndex = entryIndex)
        val visible = listState.layoutInfo.visibleItemsInfo
        val isFullyVisible = visible.any {
            it.index == index &&
                it.offset >= 0 &&
                it.offset + it.size <= listState.layoutInfo.viewportEndOffset
        }
        if (!isFullyVisible) {
            val isAbove = visible.isNotEmpty() && index < visible.first().index
            if (isAbove || visible.isEmpty()) {
                listState.scrollToItem(index = index)
            } else {
                listState.scrollToItem(index = (index - (visible.size - 2)).coerceAtLeast(minimumValue = 0))
            }
        }
    }
}

@Composable
private fun JumpToLatestButton(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    lastIndex: Int,
) {
    val scrollTarget by rememberUpdatedState(newValue = lastIndex)
    var requests by remember { mutableStateOf(value = 0) }
    LaunchedEffect(requests) {
        if (requests > 0) listState.scrollToItem(index = scrollTarget)
    }
    DefaultButton(
        onClick = { requests++ },
        modifier = modifier,
    ) {
        Text(text = BetterLogcatBundle.message("list.jumpToLatest"))
    }
}

private fun indexAt(
    listState: LazyListState,
    y: Float,
): Int {
    val visible = listState.layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return -1
    val position = y.roundToInt()
    visible.firstOrNull { position >= it.offset && position < it.offset + it.size }?.let { return it.index }
    return if (position < visible.first().offset) visible.first().index else visible.last().index
}

private fun toSwingPoint(
    coordinates: LayoutCoordinates?,
    local: Offset,
    density: Density,
): Point {
    val origin = coordinates?.positionInRoot() ?: Offset.Zero
    return Point(
        ((origin.x + local.x) / density.density).roundToInt(),
        ((origin.y + local.y) / density.density).roundToInt(),
    )
}

private fun handleKey(
    event: KeyEvent,
    state: LogcatState,
    isTextSelecting: Boolean,
    onExitTextSelection: () -> Unit,
    onContextMenu: () -> Unit,
    onIntent: (LogcatIntent) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    // Shift+F10 or the menu key opens the row menu, as in other IDE lists.
    if ((event.key == Key.F10 && event.isShiftPressed) || event.key == Key.Menu) {
        onContextMenu()
        return true
    }
    if (isTextSelecting) {
        // The text selection owns the keys (copy, Shift+arrows). Only Esc leaves it.
        if (event.key != Key.Escape) return false
        onExitTextSelection()
        return true
    }
    val isPrimary = if (SystemInfo.isMac) event.isMetaPressed else event.isCtrlPressed
    val intent = when {
        isPrimary && event.key == Key.C -> LogcatIntent.Copy(mode = state.viewOptions.defaultCopyMode)
        isPrimary && event.key == Key.A -> LogcatIntent.SelectAll
        event.key == Key.DirectionUp -> LogcatIntent.MoveSelection(delta = -1, isExtend = event.isShiftPressed)
        event.key == Key.DirectionDown -> LogcatIntent.MoveSelection(delta = 1, isExtend = event.isShiftPressed)
        event.key == Key.PageUp -> LogcatIntent.MoveSelection(delta = -PAGE_ROWS, isExtend = event.isShiftPressed)
        event.key == Key.PageDown -> LogcatIntent.MoveSelection(delta = PAGE_ROWS, isExtend = event.isShiftPressed)
        event.key == Key.MoveHome -> LogcatIntent.MoveSelection(delta = -state.entries.size, isExtend = event.isShiftPressed)
        event.key == Key.MoveEnd -> LogcatIntent.MoveSelection(delta = state.entries.size, isExtend = event.isShiftPressed)
        event.key == Key.DirectionRight -> LogcatIntent.SetSelectionExpanded(isExpanded = true)
        event.key == Key.DirectionLeft -> LogcatIntent.SetSelectionExpanded(isExpanded = false)
        event.key == Key.Enter || event.key == Key.Spacebar -> {
            if (state.selection.leadId < 0) return false
            LogcatIntent.ToggleExpanded(id = state.selection.leadId)
        }
        event.key == Key.Escape -> LogcatIntent.ClearSelection
        else -> return false
    }
    onIntent(intent)
    return true
}

/** Which cell sits at [x] in a row. In stacked rows the message is below the first line. */
private fun cellAt(
    x: Float,
    yInRow: Float,
    viewOptions: ViewOptionsUiModel,
    isStacked: Boolean,
    density: Density,
): SelectableCell = with(density) {
    if (isStacked && yInRow > STACKED_FIRST_LINE.toPx()) return SelectableCell.MESSAGE
    var edge = (ROW_HORIZONTAL_PADDING + EXPAND_SLOT).toPx()
    for (column in LogColumn.entries.filter { it in viewOptions.visibleColumns }) {
        val width = (if (column == LogColumn.LEVEL) column.defaultWidthDp else viewOptions.width(column = column)).dp.toPx()
        if (x < edge + width) {
            return when (column) {
                LogColumn.TIME -> SelectableCell.TIME
                LogColumn.PROCESS -> SelectableCell.PROCESS
                LogColumn.PACKAGE -> SelectableCell.PACKAGE
                LogColumn.TAG -> SelectableCell.TAG
                LogColumn.LEVEL -> SelectableCell.NONE
            }
        }
        edge += width
    }
    if (isStacked) SelectableCell.NONE else SelectableCell.MESSAGE
}

private val STACKED_FIRST_LINE = 20.dp

/** Text pieces of split messages, reused across recompositions. Bounded, oldest out first. */
private class PieceCache {
    private val map = object : LinkedHashMap<PieceKey, List<AnnotatedString>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PieceKey, List<AnnotatedString>>): Boolean = size > MAX_CACHED
    }

    fun getOrPut(
        key: PieceKey,
        build: () -> List<AnnotatedString>,
    ): List<AnnotatedString> = map.getOrPut(key, build)

    private companion object {
        const val MAX_CACHED = 256
    }
}

private data class PieceKey(
    val entry: LogEntry,
    val isFullyShown: Boolean,
    val prettyJson: Boolean,
    val highlighter: SearchHighlighter,
    val keywords: KeywordHighlighter,
    val colors: LogColors,
    val textColor: Color,
)

/** Splits big expanded messages into pieces. Smaller messages stay one row; they are cheap to draw. */
private fun buildLayout(
    state: LogcatState,
    textSelection: TextSelectionUiModel,
    fullyShownIds: Set<Long>,
    highlighter: SearchHighlighter,
    keywords: KeywordHighlighter,
    colors: LogColors,
    levelTextColors: Map<LogLevel, Color>,
    labels: MessageLabels,
    cache: PieceCache,
    onOpenFrame: (StackFrame) -> Unit,
    onShowAll: (Long) -> Unit,
): LogListLayout {
    val chunked = HashMap<Int, List<AnnotatedString>>()
    state.entries.forEachIndexed { index, entry ->
        if (entry.message.length < SPLIT_MIN_CHARS) return@forEachIndexed
        if (textSelection.cellFor(id = entry.id) == SelectableCell.MESSAGE) return@forEachIndexed
        if (!state.isExpanded(entry = entry)) return@forEachIndexed
        val textColor = levelTextColors.getValue(entry.level)
        val isFullyShown = entry.id in fullyShownIds
        val pieces = cache.getOrPut(
            key = PieceKey(
                entry = entry,
                isFullyShown = isFullyShown,
                prettyJson = state.viewOptions.prettyJson,
                highlighter = highlighter,
                keywords = keywords,
                colors = colors,
                textColor = textColor,
            ),
        ) {
            LogListLayout.split(
                text = buildMessageText(
                    entry = entry,
                    isExpanded = true,
                    prettyJson = state.viewOptions.prettyJson,
                    isFullyShown = isFullyShown,
                    textColor = textColor,
                    colors = colors,
                    highlighter = highlighter,
                    keywords = keywords,
                    labels = labels,
                    onOpenFrame = onOpenFrame,
                    onOpenUrl = { BrowserUtil.browse(it) },
                    onShowAll = { onShowAll(entry.id) },
                ),
                linesPerPiece = LINES_PER_PIECE,
            )
        }
        if (pieces.size > 1) chunked[index] = pieces
    }
    return LogListLayout(
        entries = state.entries,
        chunked = chunked,
    )
}

private const val SPLIT_MIN_CHARS = 2_000
private const val LINES_PER_PIECE = 20
private const val KEY_MENU_X = 40f

private class ClickTracker {
    private var lastId = -1L
    private var lastTime = 0L

    fun isDoubleClick(
        id: Long,
        timeMillis: Long,
    ): Boolean {
        val isDouble = id == lastId && timeMillis - lastTime < DOUBLE_CLICK_MILLIS
        lastId = if (isDouble) -1L else id
        lastTime = timeMillis
        return isDouble
    }
}

// region Previews

@Preview
@Composable
private fun PreviewLogList() {
    BetterLogcatPreviewTheme {
        LogList(
            state = LogcatPreviewData.logsState,
            colors = rememberLogColors(),
            onIntent = {},
            onRowMenu = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun PreviewLogListCompactNoWrap() {
    BetterLogcatPreviewTheme {
        LogList(
            state = LogcatPreviewData.logsState.copy(
                viewOptions = LogcatPreviewData.logsState.viewOptions.copy(
                    compactRows = true,
                    softWrap = false,
                ),
            ),
            colors = rememberLogColors(),
            onIntent = {},
            onRowMenu = { _, _ -> },
        )
    }
}

// endregion
