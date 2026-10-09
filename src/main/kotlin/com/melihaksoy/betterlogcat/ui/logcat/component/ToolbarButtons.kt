package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.ToggleableIconButton
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys

private val BUTTON_SIZE = 26.dp

@Composable
internal fun ToolbarIconButton(
    modifier: Modifier = Modifier,
    iconKey: IconKey,
    tooltip: String,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    Tooltip(tooltip = { Text(text = tooltip) }) {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(BUTTON_SIZE),
            enabled = isEnabled,
        ) {
            Icon(
                key = iconKey,
                contentDescription = tooltip,
            )
        }
    }
}

@Composable
internal fun ToolbarToggleButton(
    modifier: Modifier = Modifier,
    iconKey: IconKey,
    tooltip: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Tooltip(tooltip = { Text(text = tooltip) }) {
        ToggleableIconButton(
            value = isSelected,
            onValueChange = onToggle,
            modifier = modifier.size(BUTTON_SIZE),
        ) {
            Icon(
                key = iconKey,
                contentDescription = tooltip,
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun PreviewToolbarIconButton() {
    BetterLogcatPreviewTheme {
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.GC,
            tooltip = BetterLogcatBundle.message("toolbar.clear"),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewToolbarToggleButtonSelected() {
    BetterLogcatPreviewTheme {
        ToolbarToggleButton(
            iconKey = AllIconsKeys.Actions.ToggleSoftWrap,
            tooltip = BetterLogcatBundle.message("toolbar.softWrap"),
            isSelected = true,
            onToggle = {},
        )
    }
}

// endregion
