package com.melihaksoy.betterlogcat.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.intellij.ide.ui.UITheme
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfoImpl
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import androidx.compose.foundation.layout.Column
import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.filter.PackageFilter
import com.melihaksoy.betterlogcat.filter.SuggestionContext
import com.melihaksoy.betterlogcat.model.Bookmark
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.ui.logcat.LogcatContent
import com.melihaksoy.betterlogcat.ui.logcat.component.FilterBar
import com.melihaksoy.betterlogcat.ui.logcat.component.GutterUiModel
import com.melihaksoy.betterlogcat.ui.logcat.component.JsonPathBar
import com.melihaksoy.betterlogcat.ui.logcat.component.JsonPathItem
import com.melihaksoy.betterlogcat.ui.logcat.component.LogRow
import com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter
import com.melihaksoy.betterlogcat.ui.logcat.component.StickyEntryBar
import com.melihaksoy.betterlogcat.ui.logcat.model.BookmarkUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SavedFiltersUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.logcat.popup.LogcatPopups
import com.melihaksoy.betterlogcat.ui.logcat.preview.LogcatPreviewData
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.skia.EncodedImageFormat
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.UIManager

/** Dev-only: renders screens to PNG when RENDER_DIR is set. RENDER_DARK=1 renders them in a dark IDE theme. */
class RenderProbeTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        if (System.getenv("RENDER_DARK") != null) useDarkTheme()
    }

    // The headless test IDE installs no theme, so load the new UI dark theme into the Swing defaults the
    // bridge theme reads, and flip JBColor to its dark variants. LogColors follows the bridge theme's isDark.
    private fun useDarkTheme() {
        val json = UITheme::class.java.classLoader.getResourceAsStream("themes/expUI/expUI_dark.theme.json")
            ?: error("No dark theme in the platform")
        val theme = json.use {
            UITheme.loadTempThemeFromJson(
                stream = it,
                themeId = "ExperimentalDark",
            )
        }
        UIThemeLookAndFeelInfoImpl(theme).installTheme(UIManager.getLookAndFeelDefaults())
        JBColor.setDark(true)
    }

    override fun tearDown() {
        try {
            JBColor.setDark(false)
        } finally {
            super.tearDown()
        }
    }

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
                                processName = "com.example.pokedex",
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
                packageAliases = mapOf("com.example.pokedex.debug" to "VG debug"),
                levelColors = mapOf(com.melihaksoy.betterlogcat.model.LogLevel.DEBUG to 0xFF2EB67D),
                highlightRules = listOf(com.melihaksoy.betterlogcat.model.HighlightRule(pattern = "412 ms", colorArgb = 0xFFE06C75, isRegex = false)),
                presetHighlights = com.melihaksoy.betterlogcat.model.PresetHighlights(enabledIds = setOf("ktor")),
                hiddenUrlBases = listOf("https://api.example.com"),
                onIntent = {},
                onOpenIdeSettings = {},
                onEditAlias = {},
                onAddAlias = {},
                onAddUrlBase = {},
                onPickColor = { _, _ -> },
            )
        }
    }

    fun testRenderSuggestions() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val context = com.melihaksoy.betterlogcat.filter.SuggestionContext(
            packages = listOf("com.example.pokedex", "com.example.pokedex.debug"),
            tags = listOf("OkHttp", "ActivityThread"),
            processNames = emptyList(),
        )
        render(File(dir, "suggest-keys.png"), width = 1100, height = 700, isTrimmed = true) {
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
        render(File(dir, "suggest-values.png"), width = 1100, height = 700, isTrimmed = true) {
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
        render(File(dir, "saved-filters.png"), width = 1100, height = 600, isTrimmed = true) {
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
                appTags = setOf("OkHttp", "PokedexApp", "AndroidRuntime", "ActivityThread"),
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
            message = "RESPONSE: 200 OK\nMETHOD: GET\nFROM: https://pokeapi.co/api/v2/pokemon/25\nCOMMON HEADERS\n-> content-type: application/json\nBODY START\n{\"id\":25,\"name\":\"pikachu\",\"height\":4,\"weight\":60,\"base_experience\":112}\nBODY END",
        )
        val call = com.melihaksoy.betterlogcat.network.NetworkCall(
            method = "GET", url = "https://pokeapi.co/api/v2/pokemon/25",
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
            message = "RESPONSE: 200 OK\nBODY START\n" + "{\"results\":[" + (1..40).joinToString(",") { "{\"id\":$it,\"is_default\":true,\"stats\":{\"hp\":45,\"speed\":45},\"legendary\":false}" } + "]}\nBODY END",
        )
        val textEntry = LogcatPreviewData.entries[1]
        val options = LogcatPreviewData.logsState.viewOptions
        render(File(dir, "selection-json.png"), width = 2400, height = 520) {
            androidx.compose.foundation.layout.Column {
                com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                    entry = textEntry, previous = null, processName = "com.example.pokedex", isSelected = true, isExpanded = false,
                    viewOptions = options, highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                    isStacked = false, selectedCell = com.melihaksoy.betterlogcat.ui.logcat.model.SelectableCell.TAG, onToggleExpanded = {}, onOpenFrame = {},
                )
                com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                    entry = textEntry, previous = null, processName = "com.example.pokedex", isSelected = true, isExpanded = false,
                    viewOptions = options, highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                    isStacked = false, selectedCell = com.melihaksoy.betterlogcat.ui.logcat.model.SelectableCell.MESSAGE, onToggleExpanded = {}, onOpenFrame = {},
                )
                com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                    entry = jsonEntry, previous = null, processName = "com.example.pokedex", isSelected = false, isExpanded = false,
                    viewOptions = options, highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                    isStacked = false, onToggleExpanded = {}, onOpenFrame = {},
                )
                com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                    entry = jsonEntry, previous = null, processName = "com.example.pokedex", isSelected = false, isExpanded = true,
                    viewOptions = options.copy(prettyJson = false), highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                    colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                    isStacked = false, onToggleExpanded = {}, onOpenFrame = {},
                )
            }
        }
    }

    fun testRenderKtor() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val names = listOf("bulbasaur", "ivysaur", "venusaur", "charmander", "charmeleon", "charizard")
        val results = names.withIndex().joinToString(",") { (index, name) -> "{\"name\":\"$name\",\"url\":\"https:\\/\\/pokeapi.co\\/api\\/v2\\/pokemon\\/${index + 1}\\/\"}" }
        val body = "{\"count\":1302,\"next\":\"https:\\/\\/pokeapi.co\\/api\\/v2\\/pokemon?offset=6&limit=6\",\"previous\":null,\"results\":[$results]}"
        val message = listOf(
            "RESPONSE: 200", "METHOD: GET", "FROM: https://pokeapi.co/api/v2/pokemon?limit=6", "COMMON HEADERS",
            "-> cache-control: public, max-age=86400",
            "-> content-type: application/json; charset=utf-8",
            "BODY Content-Type: application/json", "BODY START", body, "BODY END",
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
        val urls = com.melihaksoy.betterlogcat.format.UrlShortener(bases = listOf("https://pokeapi.co/api/v2"))
        render(File(dir, "hidden-urls.png"), width = 2400, height = 1500) {
            androidx.compose.foundation.layout.Column {
                listOf(false, true).forEach { expanded ->
                    com.melihaksoy.betterlogcat.ui.logcat.component.LogRow(
                        entry = entry, previous = null, processName = "com.example.app.debug", isSelected = false, isExpanded = expanded,
                        viewOptions = options, highlighter = com.melihaksoy.betterlogcat.ui.logcat.component.SearchHighlighter.None,
                        colors = com.melihaksoy.betterlogcat.ui.theme.rememberLogColors(), textStyle = org.jetbrains.jewel.foundation.theme.JewelTheme.editorTextStyle,
                        isStacked = false, urls = urls, onToggleExpanded = {}, onOpenFrame = {},
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
                        com.melihaksoy.betterlogcat.network.NetworkCall(method = "GET", url = "https://pokeapi.co/api/v2/pokemon/25", requestEntryId = 99, requestEpochMillis = 0, responseEntryId = okhttp.id, statusCode = 200, durationMillis = 340),
                        com.melihaksoy.betterlogcat.network.NetworkCall(method = "GET", url = "https://pokeapi.co/api/v2/pokemon/missingno", requestEntryId = 100, requestEpochMillis = 5, responseEntryId = 101, statusCode = 404, durationMillis = 123),
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
                                entries.forEach { recorder.record(entry = it, processName = "com.example.pokedex", ignoredKeys = emptySet()) }
                                recorder.snapshot()
                            },
                        ),
                        history = listOf(
                            com.melihaksoy.betterlogcat.model.ProblemSession(
                                id = "old", title = "com.example.pokedex launched · Google Pixel 9", startedEpochMillis = System.currentTimeMillis() - 86_400_000L,
                                problems = listOf(
                                    com.melihaksoy.betterlogcat.model.SavedProblem(
                                        key = "k", kind = com.melihaksoy.betterlogcat.model.ProblemKind.ANR, title = "ANR in com.example.pokedex", detail = "Input dispatching timed out",
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
            pid = 14_302, tid = 14_302, processName = "com.example.pokedex", sample = "$title: $detail",
        )
        val kinds = com.melihaksoy.betterlogcat.model.ProblemKind.entries
        render(File(dir, "problems-panel.png"), width = 820, height = 1100) {
            com.melihaksoy.betterlogcat.ui.logcat.component.ProblemsPanel(
                problems = com.melihaksoy.betterlogcat.ui.logcat.model.ProblemsUiModel(
                    current = com.melihaksoy.betterlogcat.model.ProblemSession(
                        id = "live", title = "Google Pixel 9", startedEpochMillis = now - 600_000,
                        problems = listOf(
                            problem(kind = kinds[0], title = "IllegalStateException", detail = "Pokemon id must not be blank", tag = "AndroidRuntime", count = 1, agoSeconds = 40),
                            problem(kind = kinds[1], title = "ANR in com.example.pokedex", detail = "Input dispatching timed out", tag = "ActivityManager", count = 1, agoSeconds = 200),
                            problem(kind = kinds[2], title = "UnknownHostException", detail = "Unable to resolve host \"pokeapi.co\"", tag = "OkHttp", count = 4, agoSeconds = 90),
                            problem(kind = kinds[3], title = "DiskReadViolation", detail = "~duration=48 ms", tag = "StrictMode", count = 2, agoSeconds = 300),
                            problem(kind = kinds[4], title = "Accessing hidden method Landroid/view/View;->computeFitSystemWindows", detail = "", tag = "garage.app", count = 12, agoSeconds = 500),
                        ),
                    ),
                    history = listOf(
                        com.melihaksoy.betterlogcat.model.ProblemSession(
                            id = "old", title = "com.example.pokedex launched · Google Pixel 9", startedEpochMillis = now - 86_400_000L,
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

    fun testRenderNoWrap() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val base = LogcatPreviewData.logsState
        val scene = ImageComposeScene(width = 1600, height = 800, density = Density(density = 2f)) {
            BetterLogcatPreviewTheme {
                LogcatContent(
                    state = base.copy(viewOptions = base.viewOptions.copy(softWrap = false)),
                    popups = LogcatPopups.None,
                    paneActions = com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions.None,
                    onIntent = {},
                )
            }
        }
        scene.render(nanoTime = 0L)
        scene.render(nanoTime = 500_000_000L)
        File(dir, "no-wrap.png").writeBytes(scene.render(nanoTime = 1_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        // A sideways wheel over the list (Shift+wheel or a trackpad swipe) moves header and rows together.
        repeat(times = 30) {
            scene.sendPointerEvent(
                eventType = androidx.compose.ui.input.pointer.PointerEventType.Scroll,
                position = androidx.compose.ui.geometry.Offset(x = 800f, y = 500f),
                scrollDelta = androidx.compose.ui.geometry.Offset(x = 10f, y = 0f),
            )
        }
        // Wheel scrolling animates: draw frames until it settles.
        var frameTime = 1_000_000_000L
        repeat(times = 120) {
            frameTime += 16_000_000L
            scene.render(nanoTime = frameTime)
        }
        File(dir, "no-wrap-scrolled.png").writeBytes(scene.render(nanoTime = frameTime + 16_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        scene.close()
    }

    /** Double-click on a big collapsed response: before.png, then after.png. CLICK_X/CLICK_Y in scene pixels, SOFT_WRAP=false for sideways scroll. */
    fun testRenderDoubleClick() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val body = "{\"results\":[" + (1..30).joinToString(",") { "{\"id\":$it,\"name\":\"pokemon-$it\",\"is_default\":true}" } + "]}"
        val big = listOf("RESPONSE: 200", "METHOD: GET", "FROM: https://pokeapi.co/api/v2/pokemon?limit=20", "BODY START", body, "BODY END").joinToString("\n")
        val entries = (1L..(System.getenv("ROWS")?.toLongOrNull() ?: 48L)).map { id ->
            com.melihaksoy.betterlogcat.model.LogEntry(
                id = id, epochSeconds = 1_791_540_000L + id, nanos = 0, pid = 3776, tid = 4977,
                level = LogLevel.INFO, tag = if (id == 41L) "Ktor Client" else "Filler",
                message = if (id == 41L) big else "filler line $id",
            )
        }
        val base = LogcatPreviewData.logsState
        val softWrap = System.getenv("SOFT_WRAP") != "false"
        val scene = ImageComposeScene(width = 2400, height = 1100, density = Density(density = 2f)) {
            BetterLogcatPreviewTheme {
                LogcatContent(
                    state = base.copy(
                        entries = entries,
                        selection = com.melihaksoy.betterlogcat.ui.logcat.model.SelectionUiModel(anchorId = 41, leadId = 41, selectedIds = setOf(41L)),
                        viewOptions = base.viewOptions.copy(softWrap = softWrap, expandByDefault = true, expandJsonByDefault = System.getenv("EXPAND_JSON") == "true"),
                    ),
                    popups = LogcatPopups.None,
                    paneActions = com.melihaksoy.betterlogcat.ui.logcat.pane.PaneActions.None,
                    onIntent = {},
                )
            }
        }
        var frameTime = 0L
        fun frames(count: Int) = repeat(times = count) {
            scene.render(nanoTime = frameTime)
            frameTime += 16_000_000L
        }
        frames(count = 30)
        // SCROLL_STEPS wheel steps down, to sit inside the big message with more rows below it.
        repeat(times = System.getenv("SCROLL_STEPS")?.toIntOrNull() ?: 0) {
            scene.sendPointerEvent(
                eventType = androidx.compose.ui.input.pointer.PointerEventType.Scroll,
                position = androidx.compose.ui.geometry.Offset(x = 1200f, y = 600f),
                scrollDelta = androidx.compose.ui.geometry.Offset(x = 0f, y = 10f),
            )
            frames(count = 2)
        }
        frames(count = 60)
        File(dir, "before.png").writeBytes(scene.render(nanoTime = frameTime).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        val x = System.getenv("CLICK_X")?.toFloatOrNull()
        val y = System.getenv("CLICK_Y")?.toFloatOrNull()
        if (x != null && y != null) {
            val at = androidx.compose.ui.geometry.Offset(x = x, y = y)
            repeat(times = 2) {
                scene.sendPointerEvent(eventType = androidx.compose.ui.input.pointer.PointerEventType.Press, position = at, timeMillis = frameTime / 1_000_000L, button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                scene.sendPointerEvent(eventType = androidx.compose.ui.input.pointer.PointerEventType.Release, position = at, timeMillis = frameTime / 1_000_000L + 10, button = androidx.compose.ui.input.pointer.PointerButton.Primary)
                frames(count = 3)
            }
            frames(count = 60)
            File(dir, "after.png").writeBytes(scene.render(nanoTime = frameTime).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        scene.close()
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

    fun testRenderHttpAndFolds() {
        val dir = System.getenv("RENDER_DIR") ?: return
        val tags = listOf("System.out", "OkHttp", "Choreographer", "ActivityThread", "ViewRootImpl", "InsetsController", "ImeTracker")
        render(File(dir, "filter-folds.png"), width = 2400, height = 200) {
            FilterBar(
                filter = LogFilter().withQueryString(text = "package:mine level:I tag=:${tags.joinToString(separator = ",")} status:4xx pikachu"),
                isQueryValid = true,
                suggestionContext = SuggestionContext(
                    packages = emptyList(),
                    tags = emptyList(),
                    processNames = emptyList(),
                ),
                savedFilters = SavedFiltersUiModel(),
                colors = rememberLogColors(),
                onIntent = {},
            )
        }
        val body = "{\"count\":2,\"results\":[{\"name\":\"bulbasaur\",\"stats\":{\"hp\":45,\"speed\":45}},{\"name\":\"ivysaur\",\"stats\":{\"hp\":60,\"speed\":60}}]}"
        val entry = LogEntry(
            id = 8, epochSeconds = 1_791_540_000L, nanos = 0, pid = 3776, tid = 4977, level = LogLevel.INFO, tag = "Ktor Client",
            message = listOf("RESPONSE: 404 Not Found", "METHOD: GET", "FROM: https://pokeapi.co/api/v2/pokemon?limit=2", "BODY START", body, "BODY END").joinToString("\n"),
        )
        val call = NetworkCall(
            method = "GET", url = "https://pokeapi.co/api/v2/pokemon?limit=2",
            requestEntryId = 7, requestEpochMillis = 0, responseEntryId = 8, statusCode = 404, durationMillis = 340,
        )
        render(File(dir, "json-folds.png"), width = 2400, height = 900) {
            Column {
                listOf(false, true).forEach { expanded ->
                    LogRow(
                        entry = entry, previous = null, processName = "com.example.app.debug", isSelected = false, isExpanded = expanded,
                        viewOptions = LogcatPreviewData.logsState.viewOptions, highlighter = SearchHighlighter.None,
                        colors = rememberLogColors(), textStyle = JewelTheme.editorTextStyle, isStacked = false,
                        gutter = GutterUiModel(isBookmarked = false, problem = null, call = call),
                        jsonFolds = setOf(2), onToggleExpanded = {}, onOpenFrame = {},
                    )
                }
            }
        }
        render(File(dir, "json-path.png"), width = 1600, height = 160) {
            Column {
                StickyEntryBar(entry = entry, colors = rememberLogColors(), textStyle = JewelTheme.editorTextStyle, onCollapse = {}, onCopyJson = {}, onOpenInEditor = {})
                JsonPathBar(
                    path = listOf(JsonPathItem(block = 0, label = "{ }"), JsonPathItem(block = 1, label = "results"), JsonPathItem(block = 2, label = "[1]"), JsonPathItem(block = 3, label = "stats")),
                    colors = rememberLogColors(), textStyle = JewelTheme.editorTextStyle, onFold = {},
                )
            }
        }
    }

    // RENDER_SIZE=2400x1520 renders every screen at one size, for a matching Marketplace screenshot set.
    // isTrimmed crops to the drawn pixels: a popup draws in its own layer, the rest of the scene stays clear.
    private fun render(file: File, width: Int = 2400, height: Int = 1100, isTrimmed: Boolean = false, content: @Composable () -> Unit) {
        val size = System.getenv("RENDER_SIZE")?.split('x')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 }
        val scene = ImageComposeScene(width = size?.get(0) ?: width, height = size?.get(1) ?: height, density = Density(density = 2f)) {
            BetterLogcatPreviewTheme { content() }
        }
        scene.render()
        val image = scene.render(nanoTime = 1_000_000_000L)
        val png = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
        if (isTrimmed) {
            ImageIO.write(trimmed(image = ImageIO.read(ByteArrayInputStream(png))), "png", file)
        } else {
            file.writeBytes(png)
        }
        scene.close()
    }

    private fun trimmed(image: BufferedImage): BufferedImage {
        var left = image.width
        var top = image.height
        var right = -1
        var bottom = -1
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) ushr 24 == 0) continue
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
        if (right < 0) return image
        return image.getSubimage(left, top, right - left + 1, bottom - top + 1)
    }
}
