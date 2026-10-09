package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewOptionsUiModel
import com.melihaksoy.betterlogcat.ui.logcat.popup.IdeLogcatPopups
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.Text
import java.awt.Cursor

private val HEADER_HEIGHT = 22.dp
private val RESIZE_HANDLE_WIDTH = 6.dp

/** Column titles. Drag the right edge of a title to resize; [onResize] gets live widths, [onResizeEnd] commits. */
@Composable
internal fun LogHeader(
    modifier: Modifier = Modifier,
    viewOptions: ViewOptionsUiModel,
    colors: LogColors,
    onResize: (LogColumn, Int) -> Unit,
    onResizeEnd: (LogColumn) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .background(color = colors.headerBackground)
            .padding(horizontal = ROW_HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(EXPAND_SLOT))
        LogColumn.entries
            .filter { it in viewOptions.visibleColumns }
            .forEach { column ->
                HeaderCell(
                    column = column,
                    label = when {
                        column == LogColumn.LEVEL -> ""
                        column == LogColumn.PROCESS && !viewOptions.includeThreadId -> BetterLogcatBundle.message("column.pid")
                        else -> IdeLogcatPopups.columnLabel(column = column)
                    },
                    widthDp = viewOptions.width(column = column),
                    colors = colors,
                    onResize = { onResize(column, it) },
                    onResizeEnd = { onResizeEnd(column) },
                )
            }
        Text(
            text = BetterLogcatBundle.message("column.message"),
            color = colors.muted,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeaderCell(
    modifier: Modifier = Modifier,
    column: LogColumn,
    label: String,
    widthDp: Int,
    colors: LogColors,
    onResize: (Int) -> Unit,
    onResizeEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val width = if (column == LogColumn.LEVEL) LogColumn.LEVEL.defaultWidthDp else widthDp
    val dragged = remember { mutableFloatStateOf(value = widthDp.toFloat()) }
    val dragState = rememberDraggableState { deltaPx ->
        dragged.floatValue += with(density) { deltaPx.toDp().value }
        onResize(dragged.floatValue.toInt())
    }
    Box(
        modifier = modifier
            .width(width.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(end = CELL_GAP),
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (column.isResizable) {
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .width(RESIZE_HANDLE_WIDTH)
                    .fillMaxHeight()
                    .pointerHoverIcon(icon = PointerIcon(cursor = Cursor(Cursor.E_RESIZE_CURSOR)))
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = { dragged.floatValue = widthDp.toFloat() },
                        onDragStopped = { onResizeEnd() },
                    ),
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun PreviewLogHeader() {
    BetterLogcatPreviewTheme {
        LogHeader(
            viewOptions = ViewOptionsUiModel(),
            colors = rememberLogColors(),
            onResize = { _, _ -> },
            onResizeEnd = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLogHeaderFewColumns() {
    BetterLogcatPreviewTheme {
        LogHeader(
            viewOptions = ViewOptionsUiModel(visibleColumns = setOf(LogColumn.TIME, LogColumn.TAG, LogColumn.LEVEL)),
            colors = rememberLogColors(),
            onResize = { _, _ -> },
            onResizeEnd = {},
        )
    }
}

// endregion
