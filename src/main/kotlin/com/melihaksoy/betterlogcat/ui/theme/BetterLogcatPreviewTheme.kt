package com.melihaksoy.betterlogcat.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.theme.JewelTheme

/** Preview wrapper. Uses the IDE bridge theme, so previews follow the running IDE look. */
@Composable
fun BetterLogcatPreviewTheme(content: @Composable () -> Unit) {
    SwingBridgeTheme {
        Box(modifier = Modifier.background(color = JewelTheme.globalColors.panelBackground)) {
            content()
        }
    }
}
