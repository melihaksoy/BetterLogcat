package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import java.awt.Cursor

/**
 * A divider you can drag. [isVertical] true is a vertical line (drag left and right), false a horizontal
 * line (drag up and down). [onDrag] gets the change in dp; [onDragEnd] is the place to save.
 */
@Composable
internal fun ResizeHandle(
    modifier: Modifier = Modifier,
    isVertical: Boolean,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val currentOnDrag by rememberUpdatedState(newValue = onDrag)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val dragState = rememberDraggableState { deltaPx -> currentOnDrag(with(density) { deltaPx.toDp().value }) }
    val lineColor = if (isHovered) JewelTheme.globalColors.outlines.focused else JewelTheme.globalColors.borders.normal
    Box(
        modifier = modifier
            .then(if (isVertical) Modifier.fillMaxHeight().width(HIT_SIZE) else Modifier.fillMaxWidth().height(HIT_SIZE))
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(icon = PointerIcon(cursor = Cursor(if (isVertical) Cursor.E_RESIZE_CURSOR else Cursor.N_RESIZE_CURSOR)))
            .draggable(
                state = dragState,
                orientation = if (isVertical) Orientation.Horizontal else Orientation.Vertical,
                onDragStopped = { onDragEnd() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(if (isVertical) Modifier.fillMaxHeight().width(1.dp) else Modifier.fillMaxWidth().height(1.dp))
                .background(color = lineColor),
        )
    }
}

private val HIT_SIZE = 6.dp

// region Previews

@Preview
@Composable
private fun PreviewResizeHandle() {
    BetterLogcatPreviewTheme {
        ResizeHandle(
            modifier = Modifier.height(120.dp),
            isVertical = true,
            onDrag = {},
            onDragEnd = {},
        )
    }
}

// endregion
