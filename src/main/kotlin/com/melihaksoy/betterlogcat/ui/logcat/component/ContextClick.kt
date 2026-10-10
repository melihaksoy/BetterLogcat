package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.intellij.openapi.util.SystemInfo
import java.awt.Point

/**
 * Right-click, or Ctrl+click on macOS, on the element: [onClick] gets the Swing point for a native menu.
 * Initial pass, so a click handler on the same element never sees it. Opens on release: opened on
 * press, the release lands on the first menu item and closes the menu at once.
 */
@Composable
internal fun rememberContextClickModifier(onClick: (Point) -> Unit): Modifier {
    val anchor = rememberSwingAnchor()
    val density = LocalDensity.current
    val currentOnClick by rememberUpdatedState(newValue = onClick)
    return Modifier
        .swingAnchor(anchor = anchor)
        .pointerInput(Unit) {
            awaitEachGesture {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                if (event.type != PointerEventType.Press) return@awaitEachGesture
                val isContextClick = event.buttons.isSecondaryPressed ||
                    (SystemInfo.isMac && event.buttons.isPrimaryPressed && event.keyboardModifiers.isCtrlPressed)
                if (!isContextClick) return@awaitEachGesture
                val down = event.changes.firstOrNull() ?: return@awaitEachGesture
                event.changes.forEach { it.consume() }
                do {
                    val release = awaitPointerEvent(pass = PointerEventPass.Initial)
                    release.changes.forEach { it.consume() }
                } while (release.changes.any { it.pressed })
                currentOnClick(
                    anchor.at(
                        local = down.position,
                        density = density,
                    ),
                )
            }
        }
}
