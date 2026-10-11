package com.melihaksoy.betterlogcat.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.model.Bookmark
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.ui.logcat.LogcatContent
import com.melihaksoy.betterlogcat.ui.logcat.model.BookmarkUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
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
                        query = "package:mine level:W -tag=:art,adbd timeout ",
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

    fun testRenderBookmarks() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val base = LogcatPreviewData.logsState
        val marked = base.entries.filter { it.isCrash || it.tag == "Choreographer" }
        render(File(dir, "bookmarks.png")) {
            LogcatContent(
                state = base.copy(
                    bookmarkedIds = marked.map { it.id }.toSet(),
                    bookmarks = marked.mapIndexed { index, entry ->
                        BookmarkUiModel(
                            bookmark = Bookmark.of(
                                entry = entry,
                                processName = "com.vintagegarage.app",
                                deviceName = "Google Pixel 9",
                            ),
                            liveId = if (index == 0) BookmarkUiModel.NOT_IN_LOG else entry.id,
                        )
                    },
                    viewOptions = base.viewOptions.copy(sidePanel = SidePanel.BOOKMARKS),
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
                presetHighlights = com.melihaksoy.betterlogcat.model.PresetHighlights(enabledIds = setOf("ktor")),
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

    fun testRenderTagTree() {
        val dir = System.getenv("RENDER_DIR") ?: return
        render(File(dir, "tag-tree.png"), width = 640, height = 1000) {
            com.melihaksoy.betterlogcat.ui.logcat.component.TagsPanel(
                tagCounts = LogcatPreviewData.tagCounts,
                appTags = setOf("OkHttp", "GarageApp", "AndroidRuntime", "ActivityThread"),
                filter = LogFilter(),
                colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(),
                tagGroups = listOf(
                    com.melihaksoy.betterlogcat.model.TagGroup(name = "Group 1", tags = listOf("art", "adbd")),
                    com.melihaksoy.betterlogcat.model.TagGroup(name = "Ui", tags = listOf("ViewRootImpl", "Choreographer")),
                ),
                onIntent = {},
            )
        }
    }

    fun testRenderNetworkDetail() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val response = com.melihaksoy.betterlogcat.model.LogEntry(
            id = 2, epochSeconds = 1_791_540_000L, nanos = 0, pid = 14302, tid = 14330,
            level = com.melihaksoy.betterlogcat.model.LogLevel.INFO, tag = "Ktor Client",
            message = "RESPONSE: 200 OK\nMETHOD: GET\nFROM: https://api.vintagegarage.app/v1/listings/88213\nCOMMON HEADERS\n-> content-type: application/json\nBODY START\n{\"id\":88213,\"title\":\"1991 Mercedes-Benz 300 CE\",\"sold\":false}\nBODY END",
        )
        val call = com.melihaksoy.betterlogcat.network.NetworkCall(
            method = "GET", url = "https://api.vintagegarage.app/v1/listings/88213",
            requestEntryId = 1, requestEpochMillis = 0, responseEntryId = 2, statusCode = 200, durationMillis = 340,
        )
        render(File(dir, "network-detail.png"), width = 900, height = 900) {
            com.melihaksoy.betterlogcat.ui.logcat.component.NetworkPanel(
                calls = listOf(call),
                detail = com.melihaksoy.betterlogcat.ui.logcat.model.NetworkDetailUiModel(call = call, request = null, response = response),
                colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(),
                keywords = com.melihaksoy.betterlogcat.ui.logcat.component.KeywordHighlighter(rules = com.melihaksoy.betterlogcat.model.PresetHighlights(enabledIds = setOf("ktor")).rules()),
                onIntent = {},
            )
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

    fun testRenderGutterAndNetwork() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val base = LogcatPreviewData.logsState
        val okhttp = base.entries[2]
        render(File(dir, "gutter-network.png"), width = 2400, height = 900) {
            LogcatContent(
                state = base.copy(
                    bookmarkedIds = setOf(base.entries[1].id),
                    crashCount = 1,
                    networkCalls = listOf(
                        com.melihaksoy.betterlogcat.network.NetworkCall(method = "GET", url = "https://api.vintagegarage.app/v2/listings/88213", requestEntryId = 99, requestEpochMillis = 0, responseEntryId = okhttp.id, statusCode = 200, durationMillis = 340),
                        com.melihaksoy.betterlogcat.network.NetworkCall(method = "POST", url = "https://api.vintagegarage.app/v1/login", requestEntryId = 100, requestEpochMillis = 5, responseEntryId = 101, statusCode = 401, durationMillis = 123),
                    ),
                    viewOptions = base.viewOptions.copy(sidePanel = com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel.NETWORK),
                ),
                popups = LogcatPopups.None,
                paneActions = com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions.None,
                onIntent = {},
            )
        }
    }

    fun testRenderProblems() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val base = LogcatPreviewData.logsState
        val extra = listOf(
            com.melihaksoy.betterlogcat.model.LogEntry(
                id = 900, epochSeconds = 1_791_540_100L, nanos = 0, pid = 14302, tid = 14330,
                level = LogLevel.ERROR, tag = "RealImageLoader",
                message = "java.lang.IllegalStateException: Unable to create a fetcher that supports: https://x.io/a.png\n\tat coil3.intercept.EngineInterceptor.fetch(EngineInterceptor.kt:164)",
            ),
            com.melihaksoy.betterlogcat.model.LogEntry(
                id = 901, epochSeconds = 1_791_540_101L, nanos = 0, pid = 14302, tid = 14330,
                level = LogLevel.WARN, tag = "Choreographer", message = "Skipped 42 frames!  The application may be doing too much work on its main thread.",
            ),
        )
        val entries = base.entries + extra
        render(File(dir, "problems.png"), width = 2400, height = 900) {
            LogcatContent(
                state = base.copy(
                    entries = entries,
                    problems = com.melihaksoy.betterlogcat.ui.logcat.model.ProblemsUiModel(
                        current = com.melihaksoy.betterlogcat.model.ProblemSession(
                            id = "live", title = "Google Pixel 9", startedEpochMillis = System.currentTimeMillis(),
                            problems = com.melihaksoy.betterlogcat.model.ProblemRecorder().let { recorder ->
                                entries.forEach { recorder.record(entry = it, processName = "com.vintagegarage.app", ignoredKeys = emptySet()) }
                                recorder.snapshot()
                            },
                        ),
                        history = listOf(
                            com.melihaksoy.betterlogcat.model.ProblemSession(
                                id = "old", title = "com.vintagegarage.app launched · Google Pixel 9", startedEpochMillis = System.currentTimeMillis() - 86_400_000L,
                                problems = listOf(
                                    com.melihaksoy.betterlogcat.model.SavedProblem(
                                        key = "k", kind = com.melihaksoy.betterlogcat.model.ProblemKind.ANR, title = "ANR in com.vintagegarage.app", detail = "Input dispatching timed out",
                                        tag = "ActivityManager", count = 1, firstEpochMillis = 0, lastEpochMillis = 0, level = LogLevel.ERROR, pid = 1, tid = 1, processName = "", sample = "",
                                    ),
                                ),
                            ),
                        ),
                        frameSkips = listOf(
                            com.melihaksoy.betterlogcat.model.FrameSkip(epochMillis = 1_000L, frames = 42),
                            com.melihaksoy.betterlogcat.model.FrameSkip(epochMillis = 40_000L, frames = 120),
                            com.melihaksoy.betterlogcat.model.FrameSkip(epochMillis = 90_000L, frames = 31),
                        ),
                    ),
                    viewOptions = base.viewOptions.copy(sidePanel = com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel.PROBLEMS),
                ),
                popups = LogcatPopups.None,
                paneActions = com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions.None,
                onIntent = {},
            )
        }
    }

    fun testRenderProblemsPanel() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val now = System.currentTimeMillis()
        fun problem(
            kind: com.melihaksoy.betterlogcat.model.ProblemKind,
            title: String,
            detail: String,
            tag: String,
            count: Int,
            agoSeconds: Long,
        ) = com.melihaksoy.betterlogcat.model.SavedProblem(
            key = kind.name + tag + title, kind = kind, title = title, detail = detail, tag = tag, count = count,
            firstEpochMillis = now - agoSeconds * 1_000, lastEpochMillis = now - agoSeconds * 1_000, level = LogLevel.ERROR,
            pid = 14_302, tid = 14_302, processName = "com.vintagegarage.app", sample = "$title: $detail",
        )
        val kinds = com.melihaksoy.betterlogcat.model.ProblemKind.entries
        render(File(dir, "problems-panel.png"), width = 820, height = 1100) {
            com.melihaksoy.betterlogcat.ui.logcat.component.ProblemsPanel(
                problems = com.melihaksoy.betterlogcat.ui.logcat.model.ProblemsUiModel(
                    current = com.melihaksoy.betterlogcat.model.ProblemSession(
                        id = "live", title = "Google Pixel 9", startedEpochMillis = now - 600_000,
                        problems = listOf(
                            problem(kind = kinds[0], title = "IllegalStateException", detail = "Listing id must not be blank", tag = "AndroidRuntime", count = 1, agoSeconds = 40),
                            problem(kind = kinds[1], title = "ANR in com.vintagegarage.app", detail = "Input dispatching timed out", tag = "ActivityManager", count = 1, agoSeconds = 200),
                            problem(kind = kinds[2], title = "UnknownHostException", detail = "Unable to resolve host \"api.vintagegarage.app\"", tag = "OkHttp", count = 4, agoSeconds = 90),
                            problem(kind = kinds[3], title = "DiskReadViolation", detail = "~duration=48 ms", tag = "StrictMode", count = 2, agoSeconds = 300),
                            problem(kind = kinds[4], title = "Accessing hidden method Landroid/view/View;->computeFitSystemWindows", detail = "", tag = "garage.app", count = 12, agoSeconds = 500),
                        ),
                    ),
                    history = listOf(
                        com.melihaksoy.betterlogcat.model.ProblemSession(
                            id = "old", title = "com.vintagegarage.app launched · Google Pixel 9", startedEpochMillis = now - 86_400_000L,
                            problems = listOf(problem(kind = kinds[2], title = "SocketTimeoutException", detail = "timeout", tag = "OkHttp", count = 3, agoSeconds = 86_000)),
                        ),
                        com.melihaksoy.betterlogcat.model.ProblemSession(
                            id = "older", title = "Pixel 6a", startedEpochMillis = now - 5 * 86_400_000L,
                            problems = listOf(problem(kind = kinds[0], title = "SIGSEGV", detail = "Fatal signal 11 (SIGSEGV)", tag = "libc", count = 1, agoSeconds = 430_000)),
                        ),
                    ),
                    frameSkips = listOf(
                        com.melihaksoy.betterlogcat.model.FrameSkip(epochMillis = now - 280_000, frames = 31),
                        com.melihaksoy.betterlogcat.model.FrameSkip(epochMillis = now - 200_000, frames = 64),
                        com.melihaksoy.betterlogcat.model.FrameSkip(epochMillis = now - 120_000, frames = 120),
                        com.melihaksoy.betterlogcat.model.FrameSkip(epochMillis = now - 60_000, frames = 42),
                        com.melihaksoy.betterlogcat.model.FrameSkip(epochMillis = now, frames = 18),
                    ),
                ),
                colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(),
                onIntent = {},
            )
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

    // RENDER_SIZE=2400x1520 renders every screen at one size, for a matching Marketplace screenshot set.
    private fun render(file: File, width: Int = 2400, height: Int = 1100, content: @Composable () -> Unit) {
        val size = System.getenv("RENDER_SIZE")?.split('x')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }
        val scene = ImageComposeScene(width = size?.get(0) ?: width, height = size?.get(1) ?: height, density = Density(density = 2f)) {
            BetterLogcatPreviewTheme { content() }
        }
        scene.render()
        val image = scene.render(nanoTime = 1_000_000_000L)
        file.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        scene.close()
    }
}
