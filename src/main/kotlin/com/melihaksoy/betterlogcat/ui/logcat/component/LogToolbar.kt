package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.Device
import com.melihaksoy.betterlogcat.model.DeviceState
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewToggle
import com.melihaksoy.betterlogcat.ui.logcat.popup.LogcatPopups
import com.melihaksoy.betterlogcat.ui.logcat.preview.LogcatPreviewData
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatIcons
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.Point

/**
 * Top row: device and package pickers on the left, actions on the right in groups split by
 * dividers: log controls, side panels, expand and collapse, capture and export, settings, panes.
 */
@Composable
internal fun LogToolbar(
    modifier: Modifier = Modifier,
    state: LogcatState,
    colors: LogColors,
    popups: LogcatPopups,
    paneActions: PaneActions,
    onFind: () -> Unit = {},
    onIntent: (LogcatIntent) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 6.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        ) {
            DevicePicker(
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
                device = state.selectedDevice,
                hasDevices = state.devices.isNotEmpty(),
                colors = colors,
                onClick = popups::showDevicePicker,
            )
            PickerButton(
                modifier = Modifier.weight(
                    weight = 1f,
                    fill = false,
                ),
                label = PackageLabels.label(
                    filter = state.filter.packageFilter,
                    projectAppIds = state.projectAppIds,
                    runningPackages = state.packages,
                    aliases = state.packageAliases,
                ),
                detail = PackageLabels.detail(filter = state.filter.packageFilter),
                statusColor = Color.Unspecified,
                onClick = popups::showPackagePicker,
            )
        }
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.Find,
            tooltip = BetterLogcatBundle.message("toolbar.find"),
            onClick = onFind,
        )
        ToolbarIconButton(
            iconKey = if (state.isPaused) AllIconsKeys.Actions.Resume else AllIconsKeys.Actions.Pause,
            tooltip = BetterLogcatBundle.message(if (state.isPaused) "toolbar.resume" else "toolbar.pause"),
            onClick = { onIntent(LogcatIntent.TogglePause) },
        )
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.GC,
            tooltip = BetterLogcatBundle.message("toolbar.clear"),
            onClick = { onIntent(LogcatIntent.Clear) },
        )
        ToolbarToggleButton(
            iconKey = AllIconsKeys.Actions.ToggleSoftWrap,
            tooltip = BetterLogcatBundle.message("toolbar.softWrap"),
            isSelected = state.viewOptions.softWrap,
            onToggle = { isOn ->
                onIntent(
                    LogcatIntent.SetViewToggle(
                        toggle = ViewToggle.SOFT_WRAP,
                        isOn = isOn,
                    ),
                )
            },
        )
        ToolbarDivider()
        ToolbarToggleButton(
            iconKey = AllIconsKeys.General.Hashtag,
            tooltip = BetterLogcatBundle.message("toolbar.tags"),
            isSelected = state.viewOptions.sidePanel == SidePanel.TAGS,
            onToggle = { onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.TAGS)) },
        )
        ToolbarToggleButton(
            iconKey = AllIconsKeys.General.Web,
            tooltip = BetterLogcatBundle.message("toolbar.network"),
            isSelected = state.viewOptions.sidePanel == SidePanel.NETWORK,
            onToggle = { onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.NETWORK)) },
        )
        ToolbarToggleButton(
            // The outline tool window icon, like the other panel buttons; the filled one marks rows in the gutter.
            iconKey = AllIconsKeys.Toolwindows.ToolWindowBookmarks,
            tooltip = BetterLogcatBundle.message("toolbar.bookmarks"),
            isSelected = state.viewOptions.sidePanel == SidePanel.BOOKMARKS,
            onToggle = { onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.BOOKMARKS)) },
        )
        ToolbarToggleButton(
            iconKey = AllIconsKeys.Toolwindows.ToolWindowProblems,
            tooltip = BetterLogcatBundle.message("toolbar.problems"),
            isSelected = state.viewOptions.sidePanel == SidePanel.PROBLEMS,
            onToggle = { onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.PROBLEMS)) },
        )
        ToolbarDivider()
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.Expandall,
            tooltip = BetterLogcatBundle.message("toolbar.expandAll"),
            onClick = { onIntent(LogcatIntent.ExpandAll) },
        )
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.Collapseall,
            tooltip = BetterLogcatBundle.message("toolbar.collapseAll"),
            onClick = { onIntent(LogcatIntent.CollapseAll) },
        )
        ToolbarDivider()
        val isDeviceOnline = state.selectedDevice?.state == DeviceState.ONLINE
        ToolbarIconButton(
            iconKey = BetterLogcatIcons.Screenshot,
            tooltip = BetterLogcatBundle.message("toolbar.screenshot"),
            isEnabled = isDeviceOnline,
            onClick = popups::takeScreenshot,
        )
        ToolbarIconButton(
            iconKey = BetterLogcatIcons.ScreenRecord,
            tooltip = BetterLogcatBundle.message("toolbar.screenRecord"),
            isEnabled = isDeviceOnline,
            onClick = popups::recordScreen,
        )
        val exportAnchor = rememberSwingAnchor()
        val density = LocalDensity.current
        ToolbarIconButton(
            modifier = Modifier.swingAnchor(anchor = exportAnchor),
            iconKey = AllIconsKeys.General.Export,
            tooltip = BetterLogcatBundle.message("toolbar.export"),
            onClick = { popups.showExportMenu(at = exportAnchor.bottomLeft(density = density)) },
        )
        ToolbarDivider()
        ToolbarIconButton(
            iconKey = AllIconsKeys.General.Settings,
            tooltip = BetterLogcatBundle.message("toolbar.viewSettings"),
            onClick = popups::openViewSettings,
        )
        ToolbarDivider()
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.SplitVertically,
            tooltip = BetterLogcatBundle.message("pane.splitRight"),
            onClick = paneActions::splitRight,
        )
        ToolbarIconButton(
            iconKey = AllIconsKeys.Actions.SplitHorizontally,
            tooltip = BetterLogcatBundle.message("pane.splitDown"),
            onClick = paneActions::splitDown,
        )
        if (paneActions.canClose) {
            ToolbarIconButton(
                iconKey = AllIconsKeys.General.Close,
                tooltip = BetterLogcatBundle.message("pane.close"),
                onClick = paneActions::close,
            )
        }
    }
}

@Composable
private fun DevicePicker(
    modifier: Modifier = Modifier,
    device: Device?,
    hasDevices: Boolean,
    colors: LogColors,
    onClick: (Point) -> Unit,
) {
    val label = when {
        device != null -> device.name
        hasDevices -> BetterLogcatBundle.message("device.select")
        else -> BetterLogcatBundle.message("device.none")
    }
    val detail = when {
        device == null -> ""
        device.apiLevel.isNotEmpty() -> BetterLogcatBundle.message("device.api", device.apiLevel)
        device.state == DeviceState.UNAUTHORIZED -> BetterLogcatBundle.message("device.state.unauthorized")
        device.state == DeviceState.OFFLINE -> BetterLogcatBundle.message("device.state.offline")
        else -> ""
    }
    val statusColor = when (device?.state) {
        DeviceState.ONLINE -> colors.info
        DeviceState.UNAUTHORIZED -> colors.warn
        DeviceState.OFFLINE, DeviceState.OTHER, null -> colors.muted
    }
    PickerButton(
        modifier = modifier,
        label = label,
        detail = detail,
        statusColor = statusColor,
        onClick = onClick,
    )
}

@Composable
private fun ToolbarDivider(modifier: Modifier = Modifier) {
    Divider(
        orientation = Orientation.Vertical,
        modifier = modifier.height(16.dp),
    )
}

// region Previews

@Preview
@Composable
private fun PreviewLogToolbar() {
    BetterLogcatPreviewTheme {
        LogToolbar(
            state = LogcatPreviewData.logsState,
            colors = rememberLogColors(),
            popups = LogcatPopups.None,
            paneActions = PaneActions.None,
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLogToolbarPausedNoDevice() {
    BetterLogcatPreviewTheme {
        LogToolbar(
            state = LogcatPreviewData.logsState.copy(
                devices = emptyList(),
                selectedSerial = "",
                isPaused = true,
            ),
            colors = rememberLogColors(),
            popups = LogcatPopups.None,
            paneActions = PaneActions.None,
            onIntent = {},
        )
    }
}

// endregion
