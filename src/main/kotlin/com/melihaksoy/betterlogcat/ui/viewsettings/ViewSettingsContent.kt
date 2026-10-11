package com.melihaksoy.betterlogcat.ui.viewsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.LogColumn
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.PresetHighlights
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.component.LogHeader
import com.melihaksoy.betterlogcat.ui.logcat.component.LogRow
import com.melihaksoy.betterlogcat.ui.logcat.component.KeywordHighlighter
import com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewOptionsUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.ViewToggle
import com.melihaksoy.betterlogcat.ui.logcat.popup.IdeLogcatPopups
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text

/**
 * The view settings window: two columns of options and a live preview, like Android Studio's
 * "Logcat Format" dialog. Every change applies at once to the pane and becomes the default for new tabs.
 */
@Composable
internal fun ViewSettingsContent(
    modifier: Modifier = Modifier,
    viewOptions: ViewOptionsUiModel,
    mappingFileName: String,
    packageAliases: Map<String, String>,
    levelColors: Map<LogLevel, Long>,
    highlightRules: List<HighlightRule>,
    presetHighlights: PresetHighlights,
    onIntent: (LogcatIntent) -> Unit,
    onOpenIdeSettings: () -> Unit,
    onEditAlias: (String) -> Unit,
    onAddAlias: () -> Unit,
    onPickColor: (initial: Long, onPicked: (Long) -> Unit) -> Unit,
) {
    val baseColors = rememberLogColors()
    val colors = remember(baseColors, levelColors) { baseColors.withLevelColors(overrides = levelColors) }
    val keywords = remember(highlightRules, presetHighlights) { KeywordHighlighter(rules = presetHighlights.rules() + highlightRules) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = JewelTheme.globalColors.panelBackground)
            .padding(all = 12.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(state = rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(space = 32.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            ) {
                TimestampSection(
                    viewOptions = viewOptions,
                    onIntent = onIntent,
                )
                TagSection(
                    viewOptions = viewOptions,
                    onIntent = onIntent,
                )
                Section(title = BetterLogcatBundle.message("viewPanel.level")) {
                    ColumnCheckbox(
                        column = LogColumn.LEVEL,
                        label = BetterLogcatBundle.message("viewPanel.showLevel"),
                        viewOptions = viewOptions,
                        onIntent = onIntent,
                    )
                }
                ColorsSection(
                    levelColors = levelColors,
                    highlightRules = highlightRules,
                    presetHighlights = presetHighlights,
                    colors = colors,
                    onIntent = onIntent,
                    onPickColor = onPickColor,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            ) {
                // First in its column: these settings live in the IDE settings page, so they need a clear way there.
                IdeSettingsSection(
                    colors = colors,
                    onOpenIdeSettings = onOpenIdeSettings,
                )
                ProcessSection(
                    viewOptions = viewOptions,
                    onIntent = onIntent,
                )
                MessageSection(
                    viewOptions = viewOptions,
                    onIntent = onIntent,
                )
                RetraceSection(
                    mappingFileName = mappingFileName,
                    colors = colors,
                    onIntent = onIntent,
                )
                AliasSection(
                    packageAliases = packageAliases,
                    colors = colors,
                    onIntent = onIntent,
                    onEditAlias = onEditAlias,
                    onAddAlias = onAddAlias,
                )
            }
        }
        Preview(
            viewOptions = viewOptions,
            colors = colors,
            keywords = keywords,
        )
        Text(
            text = BetterLogcatBundle.message("viewPanel.hint"),
            color = colors.muted,
        )
    }
}

/** Buffer, adb and problem settings are in Settings › Tools › Better Logcat: what is there, and a button to open it. */
@Composable
private fun IdeSettingsSection(
    colors: LogColors,
    onOpenIdeSettings: () -> Unit,
) {
    Section(title = BetterLogcatBundle.message("viewPanel.ideSettings.title")) {
        Text(
            text = BetterLogcatBundle.message("viewPanel.ideSettings.summary"),
            color = colors.muted,
        )
        OutlinedButton(
            modifier = Modifier.padding(top = 4.dp),
            onClick = onOpenIdeSettings,
        ) {
            Text(text = BetterLogcatBundle.message("viewPanel.ideSettings"))
        }
    }
}

@Composable
private fun TimestampSection(
    viewOptions: ViewOptionsUiModel,
    onIntent: (LogcatIntent) -> Unit,
) {
    Section(title = BetterLogcatBundle.message("viewPanel.timestamp")) {
        ColumnCheckbox(
            column = LogColumn.TIME,
            label = BetterLogcatBundle.message("viewPanel.showTimestamp"),
            viewOptions = viewOptions,
            onIntent = onIntent,
        )
        TimeFormat.entries.forEach { format ->
            RadioButtonRow(
                text = IdeLogcatPopups.timeFormatLabel(format = format),
                selected = viewOptions.timeFormat == format,
                onClick = { onIntent(LogcatIntent.SetTimeFormat(format = format)) },
                modifier = Modifier.padding(start = INDENT),
                enabled = LogColumn.TIME in viewOptions.visibleColumns,
            )
        }
    }
}

@Composable
private fun TagSection(
    viewOptions: ViewOptionsUiModel,
    onIntent: (LogcatIntent) -> Unit,
) {
    Section(title = BetterLogcatBundle.message("viewPanel.tag")) {
        ColumnCheckbox(
            column = LogColumn.TAG,
            label = BetterLogcatBundle.message("viewPanel.showTag"),
            viewOptions = viewOptions,
            onIntent = onIntent,
        )
        ToggleCheckbox(
            toggle = ViewToggle.SHOW_REPEATED_TAGS,
            label = BetterLogcatBundle.message("viewPanel.showRepeatedTags"),
            viewOptions = viewOptions,
            isEnabled = LogColumn.TAG in viewOptions.visibleColumns,
            onIntent = onIntent,
        )
        ToggleCheckbox(
            toggle = ViewToggle.COLORIZE_TAGS,
            label = BetterLogcatBundle.message("viewPanel.colorizeTags"),
            viewOptions = viewOptions,
            isEnabled = LogColumn.TAG in viewOptions.visibleColumns,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun ProcessSection(
    viewOptions: ViewOptionsUiModel,
    onIntent: (LogcatIntent) -> Unit,
) {
    Section(title = BetterLogcatBundle.message("viewPanel.process")) {
        ColumnCheckbox(
            column = LogColumn.PROCESS,
            label = BetterLogcatBundle.message("viewPanel.showProcessId"),
            viewOptions = viewOptions,
            onIntent = onIntent,
        )
        ToggleCheckbox(
            toggle = ViewToggle.INCLUDE_THREAD_ID,
            label = BetterLogcatBundle.message("viewPanel.includeThreadId"),
            viewOptions = viewOptions,
            isEnabled = LogColumn.PROCESS in viewOptions.visibleColumns,
            onIntent = onIntent,
        )
    }
    Section(title = BetterLogcatBundle.message("viewPanel.package")) {
        ColumnCheckbox(
            column = LogColumn.PACKAGE,
            label = BetterLogcatBundle.message("viewPanel.showPackage"),
            viewOptions = viewOptions,
            onIntent = onIntent,
        )
        ToggleCheckbox(
            toggle = ViewToggle.SHOW_REPEATED_PACKAGES,
            label = BetterLogcatBundle.message("viewPanel.showRepeatedPackages"),
            viewOptions = viewOptions,
            isEnabled = LogColumn.PACKAGE in viewOptions.visibleColumns,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun MessageSection(
    viewOptions: ViewOptionsUiModel,
    onIntent: (LogcatIntent) -> Unit,
) {
    Section(title = BetterLogcatBundle.message("viewPanel.message")) {
        ToggleCheckbox(
            toggle = ViewToggle.SOFT_WRAP,
            label = BetterLogcatBundle.message("view.softWrap"),
            viewOptions = viewOptions,
            isEnabled = true,
            onIntent = onIntent,
        )
        ToggleCheckbox(
            toggle = ViewToggle.COMPACT_ROWS,
            label = BetterLogcatBundle.message("view.compactRows"),
            viewOptions = viewOptions,
            isEnabled = true,
            onIntent = onIntent,
        )
        ToggleCheckbox(
            toggle = ViewToggle.PRETTY_JSON,
            label = BetterLogcatBundle.message("view.prettyJson"),
            viewOptions = viewOptions,
            isEnabled = true,
            onIntent = onIntent,
        )
        ToggleCheckbox(
            toggle = ViewToggle.COPY_MESSAGE_ONLY,
            label = BetterLogcatBundle.message("view.copyMessageOnly"),
            viewOptions = viewOptions,
            isEnabled = true,
            onIntent = onIntent,
        )
        ToggleCheckbox(
            toggle = ViewToggle.EXPAND_JSON,
            label = BetterLogcatBundle.message("view.expandJson"),
            viewOptions = viewOptions,
            isEnabled = true,
            onIntent = onIntent,
        )
        CheckboxRow(
            text = BetterLogcatBundle.message("view.expandByDefault"),
            checked = viewOptions.expandByDefault,
            onCheckedChange = { isOn -> onIntent(if (isOn) LogcatIntent.ExpandAll else LogcatIntent.CollapseAll) },
        )
    }
}

@Composable
private fun RetraceSection(
    mappingFileName: String,
    colors: LogColors,
    onIntent: (LogcatIntent) -> Unit,
) {
    Section(title = BetterLogcatBundle.message("viewPanel.retrace")) {
        Text(
            text = mappingFileName.ifEmpty { BetterLogcatBundle.message("viewPanel.retrace.none") },
            color = if (mappingFileName.isEmpty()) colors.muted else JewelTheme.globalColors.text.normal,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            Link(
                text = BetterLogcatBundle.message("viewPanel.retrace.load"),
                onClick = { onIntent(LogcatIntent.ChooseMapping) },
            )
            if (mappingFileName.isNotEmpty()) {
                Link(
                    text = BetterLogcatBundle.message("viewPanel.retrace.remove"),
                    onClick = { onIntent(LogcatIntent.RemoveMapping) },
                )
            }
        }
    }
}

@Composable
private fun AliasSection(
    packageAliases: Map<String, String>,
    colors: LogColors,
    onIntent: (LogcatIntent) -> Unit,
    onEditAlias: (String) -> Unit,
    onAddAlias: () -> Unit,
) {
    Section(title = BetterLogcatBundle.message("aliases.title")) {
        if (packageAliases.isEmpty()) {
            Text(
                text = BetterLogcatBundle.message("aliases.empty"),
                color = colors.muted,
            )
        }
        packageAliases.entries.sortedBy { it.key }.forEach { (packageName, alias) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Text(text = alias)
                Text(
                    text = packageName,
                    modifier = Modifier.weight(
                        weight = 1f,
                        fill = false,
                    ),
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Link(
                    text = BetterLogcatBundle.message("aliases.edit"),
                    onClick = { onEditAlias(packageName) },
                )
                Link(
                    text = BetterLogcatBundle.message("aliases.remove"),
                    onClick = {
                        onIntent(
                            LogcatIntent.SetPackageAlias(
                                packageName = packageName,
                                alias = "",
                            ),
                        )
                    },
                )
            }
        }
        Link(
            text = BetterLogcatBundle.message("aliases.add"),
            onClick = onAddAlias,
        )
    }
}

/** Sample rows drawn with the real row composable, so the preview matches the log exactly. */
@Composable
private fun Preview(
    viewOptions: ViewOptionsUiModel,
    colors: LogColors,
    keywords: KeywordHighlighter,
) {
    val shape = RoundedCornerShape(size = 4.dp)
    Column(verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
        GroupHeader(text = BetterLogcatBundle.message("viewSettings.preview"))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PREVIEW_HEIGHT)
                .border(
                    width = 1.dp,
                    color = JewelTheme.globalColors.borders.normal,
                    shape = shape,
                )
                .background(
                    color = JewelTheme.globalColors.toolwindowBackground,
                    shape = shape,
                ),
        ) {
            Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
                LogHeader(
                    viewOptions = viewOptions,
                    colors = colors,
                    onResize = { _, _ -> },
                    onResizeEnd = {},
                )
                val entries = ViewSettingsSamples.entries
                entries.forEachIndexed { index, entry ->
                    LogRow(
                        entry = entry,
                        previous = entries.getOrNull(index = index - 1),
                        processName = ViewSettingsSamples.processNames[entry.pid].orEmpty(),
                        isSelected = false,
                        isExpanded = viewOptions.isExpandedByDefault(entry = entry) && entry.isExpandable,
                        viewOptions = viewOptions,
                        highlighter = SearchHighlighter.None,
                        keywords = keywords,
                        colors = colors,
                        textStyle = JewelTheme.editorTextStyle,
                        isStacked = false,
                        onToggleExpanded = {},
                        onOpenFrame = {},
                    )
                }
            }
        }
    }
}

private val INDENT = 22.dp
private val PREVIEW_HEIGHT = 210.dp

@Composable
private fun Section(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        GroupHeader(text = title)
        content()
    }
}

@Composable
private fun ColumnCheckbox(
    modifier: Modifier = Modifier,
    column: LogColumn,
    label: String,
    viewOptions: ViewOptionsUiModel,
    onIntent: (LogcatIntent) -> Unit,
) {
    CheckboxRow(
        text = label,
        checked = column in viewOptions.visibleColumns,
        onCheckedChange = { onIntent(LogcatIntent.ToggleColumn(column = column)) },
        modifier = modifier,
    )
}

@Composable
private fun ToggleCheckbox(
    modifier: Modifier = Modifier,
    toggle: ViewToggle,
    label: String,
    viewOptions: ViewOptionsUiModel,
    isEnabled: Boolean,
    onIntent: (LogcatIntent) -> Unit,
) {
    CheckboxRow(
        text = label,
        checked = viewOptions.isOn(toggle = toggle),
        onCheckedChange = { isOn ->
            onIntent(
                LogcatIntent.SetViewToggle(
                    toggle = toggle,
                    isOn = isOn,
                ),
            )
        },
        modifier = modifier,
        enabled = isEnabled,
    )
}

// region Previews

@Preview
@Composable
private fun PreviewViewSettingsContent() {
    BetterLogcatPreviewTheme {
        ViewSettingsContent(
            viewOptions = ViewOptionsUiModel(),
            mappingFileName = "mapping.txt",
            packageAliases = mapOf("com.vintagegarage.app.debug" to "VG debug"),
            levelColors = emptyMap(),
            highlightRules = emptyList(),
            presetHighlights = PresetHighlights(),
            onIntent = {},
            onOpenIdeSettings = {},
            onEditAlias = {},
            onAddAlias = {},
            onPickColor = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun PreviewViewSettingsContentCompact() {
    BetterLogcatPreviewTheme {
        ViewSettingsContent(
            viewOptions = ViewOptionsUiModel(
                visibleColumns = setOf(LogColumn.TIME, LogColumn.TAG, LogColumn.LEVEL),
                compactRows = true,
                timeFormat = TimeFormat.DELTA,
                showRepeatedTags = false,
            ),
            mappingFileName = "",
            packageAliases = emptyMap(),
            levelColors = emptyMap(),
            highlightRules = emptyList(),
            presetHighlights = PresetHighlights(),
            onIntent = {},
            onOpenIdeSettings = {},
            onEditAlias = {},
            onAddAlias = {},
            onPickColor = { _, _ -> },
        )
    }
}

// endregion
