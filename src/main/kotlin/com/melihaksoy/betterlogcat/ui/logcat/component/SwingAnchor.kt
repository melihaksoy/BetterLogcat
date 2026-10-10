package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import java.awt.Point
import kotlin.math.roundToInt

/** Remembers where a composable sits, so a native popup can open under it. */
internal class SwingAnchor {
    var coordinates: LayoutCoordinates? = null

    /** The Swing point of [local], a position inside the anchored composable. */
    fun at(
        local: Offset,
        density: Density,
    ): Point {
        val origin = coordinates?.positionInRoot() ?: Offset.Zero
        return Point(
            ((origin.x + local.x) / density.density).roundToInt(),
            ((origin.y + local.y) / density.density).roundToInt(),
        )
    }

    fun bottomLeft(density: Density): Point {
        val layout = coordinates ?: return Point(0, 0)
        val origin = layout.positionInRoot()
        return Point(
            (origin.x / density.density).roundToInt(),
            ((origin.y + layout.size.height) / density.density).roundToInt(),
        )
    }
}

@Composable
internal fun rememberSwingAnchor(): SwingAnchor = remember { SwingAnchor() }

internal fun Modifier.swingAnchor(anchor: SwingAnchor): Modifier = onGloballyPositioned { anchor.coordinates = it }
