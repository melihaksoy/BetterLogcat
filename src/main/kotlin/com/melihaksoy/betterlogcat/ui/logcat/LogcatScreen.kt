package com.melihaksoy.betterlogcat.ui.logcat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.DeviceState
import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.filter.SuggestionContext
import com.melihaksoy.betterlogcat.ui.logcat.component.EmptyState
import com.melihaksoy.betterlogcat.ui.logcat.component.FilterBar
import com.melihaksoy.betterlogcat.ui.logcat.component.LogList
import com.melihaksoy.betterlogcat.ui.logcat.component.LogToolbar
import com.melihaksoy.betterlogcat.ui.logcat.component.PackageLabels
import com.melihaksoy.betterlogcat.ui.logcat.component.NetworkPanel
import com.melihaksoy.betterlogcat.ui.logcat.component.TagsPanel
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions
import com.melihaksoy.betterlogcat.ui.logcat.component.StatusLine
import com.melihaksoy.betterlogcat.ui.logcat.model.LogcatContentUiModel
import com.melihaksoy.betterlogcat.ui.logcat.popup.IdeLogcatPopups
import com.melihaksoy.betterlogcat.ui.logcat.popup.LogcatPopups
import com.melihaksoy.betterlogcat.ui.logcat.preview.LogcatPreviewData
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.LocalComponent
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.icons.AllIconsKeys

private val SIDE_PANEL_WIDTH = 280.dp
private val NETWORK_PANEL_WIDTH = 420.dp

@Composable
fun LogcatScreen(
    modifier: Modifier = Modifier,
    viewModel: LogcatViewModel,
    project: Project,
    paneActions: PaneActions,
) {
    val state by viewModel.state.collectAsState()
    val currentState by rememberUpdatedState(newValue = state)
    val component = LocalComponent.current
    val popups = remember(viewModel, project, component) {
        IdeLogcatPopups(
            project = project,
            owner = { component },
            state = { currentState },
            stateFlow = viewModel.state,
            onIntent = viewModel::onIntent,
        )
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LogcatEvent.ShowExportDialog -> showExportDialog(
                    project = project,
                    defaultFileName = event.defaultFileName,
                    onIntent = viewModel::onIntent,
                )
                is LogcatEvent.ShowStatus -> WindowManager.getInstance().getStatusBar(project)?.info = event.message
                LogcatEvent.ShowMappingChooser -> showMappingChooser(
                    project = project,
                    onIntent = viewModel::onIntent,
                )
            }
        }
    }
    LogcatContent(
        modifier = modifier,
        state = state,
        popups = popups,
        paneActions = paneActions,
        onIntent = viewModel::onIntent,
    )
}

@Composable
internal fun LogcatContent(
    modifier: Modifier = Modifier,
    state: LogcatState,
    popups: LogcatPopups,
    paneActions: PaneActions,
    onIntent: (LogcatIntent) -> Unit,
) {
    var isFindOpen by remember { mutableStateOf(value = false) }
    val baseColors = rememberLogColors()
    val colors = remember(baseColors, state.levelColors) { baseColors.withLevelColors(overrides = state.levelColors) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = JewelTheme.globalColors.toolwindowBackground),
    ) {
        LogToolbar(
            state = state,
            colors = colors,
            popups = popups,
            paneActions = paneActions,
            onFind = { isFindOpen = true },
            onIntent = onIntent,
        )
        val suggestionContext = remember(state.packages, state.tagCounts, state.processNames) {
            SuggestionContext(
                packages = state.packages,
                tags = state.tagCounts.entries.sortedByDescending { it.value }.map { it.key },
                processNames = state.processNames.values.distinct().sorted(),
            )
        }
        FilterBar(
            filter = state.filter,
            isQueryValid = state.isQueryValid,
            suggestionContext = suggestionContext,
            savedFilters = state.savedFilters,
            colors = colors,
            onIntent = onIntent,
        )
        Divider(orientation = Orientation.Horizontal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
            ) {
                MainArea(
                    state = state,
                    colors = colors,
                    popups = popups,
                    isFindOpen = isFindOpen,
                    onFindOpenChange = { isFindOpen = it },
                    onIntent = onIntent,
                )
            }
            when (state.viewOptions.sidePanel) {
                SidePanel.NONE -> Unit
                SidePanel.NETWORK -> {
                    Divider(orientation = Orientation.Vertical)
                    NetworkPanel(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(NETWORK_PANEL_WIDTH),
                        calls = state.networkCalls,
                        colors = colors,
                        onIntent = onIntent,
                    )
                }
                SidePanel.TAGS -> {
                    Divider(orientation = Orientation.Vertical)
                    val appTags = remember(state.tagPids, state.processNames, state.projectAppIds) {
                        appTags(state = state)
                    }
                    TagsPanel(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(SIDE_PANEL_WIDTH),
                        tagCounts = state.tagCounts,
                        tagRates = state.tagRates,
                        appTags = appTags,
                        filter = state.filter,
                        colors = colors,
                        onIntent = onIntent,
                    )
                }
            }
        }
        Divider(orientation = Orientation.Horizontal)
        StatusLine(
            state = state,
            colors = colors,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun MainArea(
    modifier: Modifier = Modifier,
    state: LogcatState,
    colors: LogColors,
    popups: LogcatPopups,
    isFindOpen: Boolean,
    onFindOpenChange: (Boolean) -> Unit,
    onIntent: (LogcatIntent) -> Unit,
) {
    when (val content = state.content) {
        LogcatContentUiModel.Loading -> EmptyState(
            modifier = modifier,
            iconKey = AllIconsKeys.General.Information,
            title = BetterLogcatBundle.message("empty.loading.title"),
            description = "",
            actionLabel = "",
            onAction = {},
        )
        LogcatContentUiModel.AdbNotFound -> EmptyState(
            modifier = modifier,
            iconKey = AllIconsKeys.General.Warning,
            title = BetterLogcatBundle.message("empty.adb.title"),
            description = BetterLogcatBundle.message("empty.adb.description"),
            actionLabel = BetterLogcatBundle.message("empty.adb.action"),
            onAction = popups::openSettings,
        )
        LogcatContentUiModel.NoDevices -> EmptyState(
            modifier = modifier,
            iconKey = AllIconsKeys.General.Information,
            title = BetterLogcatBundle.message("empty.noDevices.title"),
            description = BetterLogcatBundle.message("empty.noDevices.description"),
            actionLabel = "",
            onAction = {},
        )
        is LogcatContentUiModel.DeviceNotReady -> EmptyState(
            modifier = modifier,
            iconKey = AllIconsKeys.General.Warning,
            title = BetterLogcatBundle.message("empty.notReady.title", content.deviceName),
            description = BetterLogcatBundle.message(
                if (content.state == DeviceState.UNAUTHORIZED) "empty.notReady.unauthorized" else "empty.notReady.offline",
            ),
            actionLabel = "",
            onAction = {},
        )
        is LogcatContentUiModel.Logs -> if (state.entries.isEmpty()) {
            EmptyLogs(
                modifier = modifier,
                state = state,
                onIntent = onIntent,
            )
        } else {
            LogList(
                modifier = modifier,
                state = state,
                colors = colors,
                isFindOpen = isFindOpen,
                onFindOpenChange = onFindOpenChange,
                onIntent = onIntent,
                onGutterMenu = { entry, gutter, point ->
                    popups.showGutterMenu(
                        entry = entry,
                        gutter = gutter,
                        at = point,
                    )
                },
                onOpenInEditor = popups::openInEditor,
                onRowMenu = { entry, point ->
                    popups.showRowMenu(
                        entry = entry,
                        at = point,
                    )
                },
            )
        }
    }
}

/** Tags logged by a process of the open project. */
internal fun appTags(state: LogcatState): Set<String> {
    if (state.projectAppIds.isEmpty()) return emptySet()
    return state.tagPids
        .filterValues { pids ->
            pids.any { pid ->
                // Unknown pids (no process name yet) are skipped, never looked up as null.
                val packageName = state.processNames[pid]?.substringBefore(':') ?: return@any false
                packageName in state.projectAppIds
            }
        }
        .keys
}

@Composable
private fun EmptyLogs(
    modifier: Modifier = Modifier,
    state: LogcatState,
    onIntent: (LogcatIntent) -> Unit,
) {
    val packageFilter = state.filter.packageFilter
    val isWaitingForPackage = !state.filter.hasNarrowing &&
        (packageFilter is PackageFilter.Package || (packageFilter == PackageFilter.ProjectApps && state.projectAppIds.isNotEmpty()))
    val title = when {
        isWaitingForPackage -> BetterLogcatBundle.message(
            "empty.waitingForPackage.title",
            PackageLabels.label(
                filter = packageFilter,
                projectAppIds = state.projectAppIds,
                runningPackages = state.packages,
                aliases = state.packageAliases,
            ),
        )
        state.filter.hasNarrowing && state.totalCount > 0 -> BetterLogcatBundle.message("empty.noMatch.title")
        else -> BetterLogcatBundle.message("empty.waiting.title")
    }
    EmptyState(
        modifier = modifier,
        iconKey = AllIconsKeys.General.Information,
        title = title,
        description = if (isWaitingForPackage) BetterLogcatBundle.message("empty.waitingForPackage.description") else "",
        actionLabel = when {
            isWaitingForPackage -> BetterLogcatBundle.message("filter.package.showAll")
            state.filter.hasNarrowing -> BetterLogcatBundle.message("filter.reset")
            else -> ""
        },
        onAction = {
            onIntent(if (isWaitingForPackage) LogcatIntent.SelectPackage(packageFilter = PackageFilter.All) else LogcatIntent.ResetFilters)
        },
    )
}

/** Opens a file chooser for `mapping.txt`, starting in the project's R8 output folder when it exists. */
private fun showMappingChooser(
    project: Project,
    onIntent: (LogcatIntent) -> Unit,
) {
    ApplicationManager.getApplication().invokeLater {
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("txt")
            .withTitle(BetterLogcatBundle.message("mapping.choose.title"))
        val start = project.basePath
            ?.let { base -> listOf("app/build/outputs/mapping", "build/outputs/mapping").map { File(base, it) }.firstOrNull { it.isDirectory } }
            ?.let { LocalFileSystem.getInstance().findFileByIoFile(it) }
        FileChooser.chooseFile(descriptor, project, start) { chosen ->
            onIntent(LogcatIntent.LoadMapping(file = File(chosen.path)))
        }
    }
}

private fun showExportDialog(
    project: Project,
    defaultFileName: String,
    onIntent: (LogcatIntent) -> Unit,
) {
    ApplicationManager.getApplication().invokeLater {
        val descriptor = FileSaverDescriptor(
            BetterLogcatBundle.message("export.title"),
            BetterLogcatBundle.message("export.description"),
            "txt",
        )
        val wrapper = FileChooserFactory.getInstance()
            .createSaveFileDialog(descriptor, project)
            .save(defaultFileName)
        wrapper?.file?.let { onIntent(LogcatIntent.ExportTo(file = it)) }
    }
}

// region Previews

@Preview
@Composable
private fun PreviewLogcatContentLogs() {
    BetterLogcatPreviewTheme {
        LogcatContent(
            state = LogcatPreviewData.logsState,
            popups = LogcatPopups.None,
            paneActions = PaneActions.None,
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLogcatContentFiltered() {
    BetterLogcatPreviewTheme {
        LogcatContent(
            state = LogcatPreviewData.filteredState,
            popups = LogcatPopups.None,
            paneActions = PaneActions.None,
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLogcatContentLoading() {
    BetterLogcatPreviewTheme {
        LogcatContent(
            state = LogcatState(),
            popups = LogcatPopups.None,
            paneActions = PaneActions.None,
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLogcatContentAdbNotFound() {
    BetterLogcatPreviewTheme {
        LogcatContent(
            state = LogcatState(content = LogcatContentUiModel.AdbNotFound),
            popups = LogcatPopups.None,
            paneActions = PaneActions.None,
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewLogcatContentUnauthorized() {
    BetterLogcatPreviewTheme {
        LogcatContent(
            state = LogcatState(
                content = LogcatContentUiModel.DeviceNotReady(
                    deviceName = LogcatPreviewData.pixel.name,
                    state = DeviceState.UNAUTHORIZED,
                ),
                devices = listOf(LogcatPreviewData.pixel.copy(state = DeviceState.UNAUTHORIZED)),
                selectedSerial = LogcatPreviewData.pixel.serial,
            ),
            popups = LogcatPopups.None,
            paneActions = PaneActions.None,
            onIntent = {},
        )
    }
}

// endregion
