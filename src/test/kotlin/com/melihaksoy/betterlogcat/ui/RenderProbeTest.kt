package com.melihaksoy.betterlogcat.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.ui.logcat.LogcatContent
import com.melihaksoy.betterlogcat.ui.logcat.popup.LogcatPopups
import com.melihaksoy.betterlogcat.ui.logcat.preview.LogcatPreviewData
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Dev-only: renders screens to PNG when RENDER_DIR is set. */
class RenderProbeTest : BasePlatformTestCase() {

    fun testRender() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val base = LogcatPreviewData.logsState
        render(File(dir, "tabs-tags.png")) {
            LogcatContent(
                state = base.copy(
                    filter = LogFilter(
                        levels = setOf(LogLevel.DEBUG, LogLevel.WARN, LogLevel.ERROR),
                        query = "package:mine -tag:art level:W timeout ",
                        excludedTags = listOf("art", "adbd"),
                    ),
                    viewOptions = base.viewOptions.copy(
                        sidePanel = com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel.TAGS,
                        showRepeatedTags = false,
                        includeThreadId = false,
                        timeFormat = TimeFormat.DELTA,
                        columnWidths = base.viewOptions.columnWidths + (com.melihaksoy.betterlogcat.model.LogColumn.TIME to TimeFormat.DELTA.widthDp),
                    ),
                ),
                popups = LogcatPopups.None,
                paneActions = object : com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions {
                    override val canClose = true
                    override fun splitRight() = Unit
                    override fun splitDown() = Unit
                    override fun close() = Unit
                },
                onIntent = {},
            )
        }
    }

    fun testRenderViewSettings() {
        val dir = System.getenv("RENDER_DIR") ?: return
        render(File(dir, "view-settings.png"), width = 1880, height = 1520) {
            com.melihaksoy.betterlogcat.ui.viewsettings.ViewSettingsContent(
                viewOptions = LogcatPreviewData.logsState.viewOptions.copy(showRepeatedTags = false, includeThreadId = false),
                mappingFileName = "",
                packageAliases = mapOf("com.vintagegarage.app.debug" to "VG debug"),
                levelColors = mapOf(com.melihaksoy.betterlogcat.model.LogLevel.DEBUG to 0xFF2EB67D),
                highlightRules = listOf(com.melihaksoy.betterlogcat.model.HighlightRule(pattern = "412 ms", colorArgb = 0xFFE06C75, isRegex = false)),
                onIntent = {},
                onOpenIdeSettings = {},
                onEditAlias = {},
                onAddAlias = {},
                onPickColor = { _, _ -> },
            )
        }
    }

    fun testRenderSuggestions() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val context = com.melihaksoy.betterlogcat.filter.SuggestionContext(
            packages = listOf("com.vintagegarage.app", "com.vintagegarage.app.debug"),
            tags = listOf("OkHttp", "ActivityThread"),
            processNames = emptyList(),
        )
        render(File(dir, "suggest-keys.png"), width = 1100, height = 700) {
            androidx.compose.foundation.layout.Box {
                com.melihaksoy.betterlogcat.ui.logcat.component.SearchSuggestionsPopup(
                    suggestions = com.melihaksoy.betterlogcat.filter.QuerySuggestions.suggest(text = "p", cursor = 1, context = context),
                    selectedIndex = 0,
                    width = 500.dp,
                    offsetY = 0,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(),
                    onPick = {},
                    onDismiss = {},
                )
            }
        }
        render(File(dir, "suggest-values.png"), width = 1100, height = 700) {
            androidx.compose.foundation.layout.Box {
                com.melihaksoy.betterlogcat.ui.logcat.component.SearchSuggestionsPopup(
                    suggestions = com.melihaksoy.betterlogcat.filter.QuerySuggestions.suggest(text = "package:", cursor = 8, context = context),
                    selectedIndex = 0,
                    width = 500.dp,
                    offsetY = 0,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(),
                    onPick = {},
                    onDismiss = {},
                )
            }
        }
    }

    fun testRenderSavedFilters() {
        val dir = System.getenv("RENDER_DIR") ?: return
        render(File(dir, "saved-filters.png"), width = 1100, height = 600) {
            androidx.compose.foundation.layout.Box {
                com.melihaksoy.betterlogcat.ui.logcat.component.SavedFiltersPopup(
                    savedFilters = com.melihaksoy.betterlogcat.ui.logcat.model.SavedFiltersUiModel(
                        favorites = listOf("name:Network package:mine tag:OkHttp"),
                        history = listOf("package:mine -tag:art", "package:mine level:error"),
                        counts = mapOf("name:Network package:mine tag:OkHttp" to 100, "package:mine -tag:art" to 42, "package:mine level:error" to 3),
                    ),
                    currentQuery = "package:mine is:crash",
                    width = 520.dp,
                    offsetY = 0,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(),
                    onApply = {},
                    onToggleFavorite = {},
                    onRemove = {},
                    onDismiss = {},
                )
            }
        }
    }

    fun testRenderSelectionAndJson() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val jsonEntry = com.melihaksoy.betterlogcat.model.LogEntry(
            id = 99, epochSeconds = 1_791_540_000L, nanos = 0, pid = 14302, tid = 14330,
            level = com.melihaksoy.betterlogcat.model.LogLevel.INFO, tag = "Ktor Client",
            message = "RESPONSE: 200 OK\nBODY START\n" + "{\"items\":[" + (1..40).joinToString(",") { "{\"id\":$it,\"title\":\"1991 Mercedes-Benz 300 CE\",\"price\":{\"value\":30000,\"currency\":\"EUR\"},\"sold\":false}" } + "]}\nBODY END",
        )
        val textEntry = LogcatPreviewData.entries[1]
        val options = LogcatPreviewData.logsState.viewOptions
        render(File(dir, "selection-json.png"), width = 2400, height = 520) {
            androidx.compose.foundation.layout.Column {
                com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                    entry = textEntry, previous = null, processName = "com.vintagegarage.app", isSelected = true, isExpanded = false,
                    viewOptions = options, highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                    isStacked = false, selectedCell = com.melihaksoy.betterlogcat.ui.logcat.model.SelectableCell.TAG, onToggleExpanded = {}, onOpenFrame = {},
                )
                com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                    entry = textEntry, previous = null, processName = "com.vintagegarage.app", isSelected = true, isExpanded = false,
                    viewOptions = options, highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                    isStacked = false, selectedCell = com.melihaksoy.betterlogcat.ui.logcat.model.SelectableCell.MESSAGE, onToggleExpanded = {}, onOpenFrame = {},
                )
                com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                    entry = jsonEntry, previous = null, processName = "com.vintagegarage.app", isSelected = false, isExpanded = false,
                    viewOptions = options, highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                    isStacked = false, onToggleExpanded = {}, onOpenFrame = {},
                )
                com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                    entry = jsonEntry, previous = null, processName = "com.vintagegarage.app", isSelected = false, isExpanded = true,
                    viewOptions = options.copy(prettyJson = false), highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                    isStacked = false, onToggleExpanded = {}, onOpenFrame = {},
                )
            }
        }
    }

    fun testRenderKtor() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val body = "{\"data\":[" + (1..6).joinToString(",") { "{\"id\":\"C205007$it\",\"listing_type\":\"basic\",\"vehicle\":{\"make\":{\"name\":\"Volkswagen\"},\"year\":\"1977\"},\"thumbnail\":\"https:\\/\\/assets.example.com\\/uploads\\/$it.jpg\"}" } + "]}"
        val message = listOf(
            "RESPONSE: 200", "METHOD: GET", "FROM: https://staging.example.com/app-api/v1/search/top-picks", "COMMON HEADERS",
            "-> cache-control: no-cache, private",
            "-> nel: {\"report_to\":\"cf-nel\",\"success_fraction\":0.0,\"max_age\":604800}",
            "-> server: cloudflare", "BODY Content-Type: application/json", "BODY START", body, "BODY END",
        ).joinToString("\n")
        val entry = com.melihaksoy.betterlogcat.model.LogEntry(
            id = 7, epochSeconds = 1_791_540_000L, nanos = 0, pid = 3776, tid = 4977,
            level = com.melihaksoy.betterlogcat.model.LogLevel.INFO, tag = "Ktor Client", message = message,
        )
        val options = LogcatPreviewData.logsState.viewOptions
        render(File(dir, "ktor.png"), width = 2400, height = 1500) {
            androidx.compose.foundation.layout.Column {
                listOf(false, true).forEach { expanded ->
                    com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                        entry = entry, previous = null, processName = "com.example.app.debug", isSelected = false, isExpanded = expanded,
                        viewOptions = options, highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                        colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                        isStacked = false, onToggleExpanded = {}, onOpenFrame = {},
                    )
                }
            }
        }
    }

    fun testRenderNarrow() {
        val dir = System.getenv("RENDER_DIR") ?: return
        render(File(dir, "narrow.png"), width = 1300, height = 500) {
            LogcatContent(
                state = LogcatPreviewData.logsState,
                popups = LogcatPopups.None,
                paneActions = object : com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions {
                    override val canClose = true
                    override fun splitRight() = Unit
                    override fun splitDown() = Unit
                    override fun close() = Unit
                },
                onIntent = {},
            )
        }
    }

    private fun render(file: File, width: Int = 2400, height: Int = 1100, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width = width, height = height, density = Density(density = 2f)) {
            BetterLogcatPreviewTheme { content() }
        }
        scene.render()
        val image = scene.render(nanoTime = 1_000_000_000L)
        file.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        scene.close()
    }
}
