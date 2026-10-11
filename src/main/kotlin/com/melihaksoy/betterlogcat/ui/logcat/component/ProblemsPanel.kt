package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.FrameSkip
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.ProblemKind
import com.melihaksoy.betterlogcat.model.ProblemSession
import com.melihaksoy.betterlogcat.model.SavedProblem
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.model.ProblemsUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.Point
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Problems of this pane's live session (crashes, ANRs, exceptions, StrictMode violations, warnings),
 * one row per problem with its count, and the saved sessions before it. Problems stay after Clear
 * and a restart until the user deletes them: per problem, per session, or by age from the ⋮ menu.
 * Click a row to go to its newest line, again for older ones; a line no longer in the log opens its
 * saved copy. Right-click a row or a session for more.
 */
@Composable
internal fun ProblemsPanel(
    modifier: Modifier = Modifier,
    problems: ProblemsUiModel,
    colors: LogColors,
    onProblemMenu: (ProblemSession, SavedProblem, Point) -> Unit = { _, _, _ -> },
    onSessionMenu: (ProblemSession, Point) -> Unit = { _, _ -> },
    onPanelMenu: (Point) -> Unit = {},
    onIntent: (LogcatIntent) -> Unit,
) {
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString().trim()
    val current = remember(problems.current, query) { problems.current?.filtered(query = query) }
    val history = remember(problems.history, query) {
        problems.history.map { it.filtered(query = query) }.filter { query.isEmpty() || it.problems.isNotEmpty() }
    }
    var collapsedKinds by remember { mutableStateOf(value = emptySet<ProblemKind>()) }
    var expandedSessions by remember { mutableStateOf(value = emptySet<String>()) }
    // How many lines back each problem's next click goes: 0 is the newest line.
    var cursors by remember { mutableStateOf(value = emptyMap<String, Int>()) }
    val onProblemClick = { session: ProblemSession, problem: SavedProblem ->
        val cursorKey = session.id + problem.key
        val back = cursors[cursorKey] ?: 0
        cursors = cursors + (cursorKey to back + 1)
        onIntent(
            LogcatIntent.JumpToProblem(
                sessionId = session.id,
                key = problem.key,
                back = back,
            ),
        )
    }
    Column(modifier = modifier.background(color = JewelTheme.globalColors.panelBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 10.dp,
                    end = 4.dp,
                    top = 6.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = BetterLogcatBundle.message("problems.title", problems.currentCount),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            val anchor = rememberSwingAnchor()
            val density = LocalDensity.current
            ToolbarIconButton(
                modifier = Modifier.swingAnchor(anchor = anchor),
                iconKey = AllIconsKeys.Actions.More,
                tooltip = BetterLogcatBundle.message("problems.more"),
                onClick = { onPanelMenu(anchor.bottomLeft(density = density)) },
            )
            ToolbarIconButton(
                iconKey = AllIconsKeys.General.Close,
                tooltip = BetterLogcatBundle.message("problems.close"),
                onClick = { onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.PROBLEMS)) },
            )
        }
        TextField(
            state = searchState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                ),
            placeholder = { Text(text = BetterLogcatBundle.message("problems.search")) },
            leadingIcon = {
                Icon(
                    key = AllIconsKeys.Actions.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
            },
        )
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {
                if (problems.frameSkips.isNotEmpty() && query.isEmpty()) {
                    item(key = "frames") {
                        FrameDropsCard(
                            skips = problems.frameSkips,
                            colors = colors,
                        )
                    }
                }
                if (current != null) {
                    item(key = "current") {
                        SessionHeaderRow(
                            session = current,
                            label = BetterLogcatBundle.message("problems.session.current"),
                            isExpanded = null,
                            colors = colors,
                            onClick = {},
                            onMenu = { point -> onSessionMenu(current, point) },
                            onDelete = null,
                        )
                    }
                    if (current.problems.isEmpty()) {
                        item(key = "current-empty") {
                            Text(
                                text = BetterLogcatBundle.message(if (query.isEmpty()) "problems.empty" else "problems.noMatch"),
                                modifier = Modifier.padding(all = 10.dp),
                                color = colors.muted,
                            )
                        }
                    }
                    val byKind = current.problems.groupBy { it.kind }
                    ProblemKind.entries.forEach { kind ->
                        val section = byKind[kind] ?: return@forEach
                        val isCollapsed = kind in collapsedKinds
                        item(key = "kind-" + kind.name) {
                            KindHeaderRow(
                                title = BetterLogcatBundle.message(sectionTitleKey(kind = kind), section.size),
                                isCollapsed = isCollapsed,
                                colors = colors,
                                onToggle = { collapsedKinds = if (isCollapsed) collapsedKinds - kind else collapsedKinds + kind },
                            )
                        }
                        if (!isCollapsed) {
                            problemRows(
                                session = current,
                                problems = section,
                                colors = colors,
                                indent = 0.dp,
                                onClick = onProblemClick,
                                onMenu = onProblemMenu,
                            )
                        }
                    }
                }
                if (history.isNotEmpty()) {
                    item(key = "history") {
                        KindHeaderRow(
                            title = BetterLogcatBundle.message("problems.history", history.size),
                            isCollapsed = null,
                            colors = colors,
                            onToggle = {},
                        )
                    }
                    history.forEach { session ->
                        val isExpanded = session.id in expandedSessions || query.isNotEmpty()
                        item(key = "session-" + session.id) {
                            SessionHeaderRow(
                                session = session,
                                label = null,
                                isExpanded = isExpanded,
                                colors = colors,
                                onClick = {
                                    expandedSessions = if (session.id in expandedSessions) expandedSessions - session.id else expandedSessions + session.id
                                },
                                onMenu = { point -> onSessionMenu(session, point) },
                                onDelete = { onIntent(LogcatIntent.DeleteProblemSession(id = session.id)) },
                            )
                        }
                        if (isExpanded) {
                            problemRows(
                                session = session,
                                problems = session.problems,
                                colors = colors,
                                indent = SESSION_INDENT,
                                onClick = onProblemClick,
                                onMenu = onProblemMenu,
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                scrollState = listState,
                modifier = Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
        }
    }
}

private fun LazyListScope.problemRows(
    session: ProblemSession,
    problems: List<SavedProblem>,
    colors: LogColors,
    indent: Dp,
    onClick: (ProblemSession, SavedProblem) -> Unit,
    onMenu: (ProblemSession, SavedProblem, Point) -> Unit,
) {
    items(
        items = problems,
        key = { session.id + it.key },
    ) { problem ->
        ProblemRow(
            problem = problem,
            colors = colors,
            indent = indent,
            onClick = { onClick(session, problem) },
            onMenu = { point -> onMenu(session, problem, point) },
        )
    }
}

private fun ProblemSession.filtered(query: String): ProblemSession = if (query.isEmpty()) {
    this
} else {
    copy(
        problems = problems.filter {
            it.title.contains(other = query, ignoreCase = true) ||
                it.detail.contains(other = query, ignoreCase = true) ||
                it.tag.contains(other = query, ignoreCase = true)
        },
    )
}

private fun sectionTitleKey(kind: ProblemKind): String = when (kind) {
    ProblemKind.CRASH -> "problems.section.crash"
    ProblemKind.ANR -> "problems.section.anr"
    ProblemKind.EXCEPTION -> "problems.section.exception"
    ProblemKind.STRICT_MODE -> "problems.section.strictMode"
    ProblemKind.WARNING -> "problems.section.warning"
}

/** The gutter icon of each kind, so a row here looks like the line it opens. */
internal fun problemIcon(kind: ProblemKind): IconKey = when (kind) {
    ProblemKind.CRASH -> AllIconsKeys.General.Error
    ProblemKind.ANR -> AllIconsKeys.Debugger.ThreadFrozen
    // The debugger's exception breakpoint: a thrown exception, set apart from a crash.
    ProblemKind.EXCEPTION -> AllIconsKeys.Debugger.Db_exception_breakpoint
    ProblemKind.STRICT_MODE -> AllIconsKeys.Actions.StopWatch
    ProblemKind.WARNING -> AllIconsKeys.General.Warning
}

/** "Today 22:24", "Yesterday 09:10" or "11 Oct 22:24", in the user's zone. */
private fun sessionTime(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val today = LocalDate.now(zone)
    val clock = time.format(TIME_FORMAT)
    return when (time.toLocalDate()) {
        today -> BetterLogcatBundle.message("problems.today", clock)
        today.minusDays(1) -> BetterLogcatBundle.message("problems.yesterday", clock)
        else -> time.format(DATE_TIME_FORMAT)
    }
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("d MMM HH:mm")
private val SESSION_INDENT = 14.dp
private const val FRAME_BUCKETS = 30
private const val FRAME_BUCKET_MILLIS = 10_000L

@Composable
private fun KindHeaderRow(
    title: String,
    isCollapsed: Boolean?,
    colors: LogColors,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colors.headerBackground)
            .then(if (isCollapsed != null) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        if (isCollapsed != null) {
            Icon(
                key = if (isCollapsed) AllIconsKeys.General.ChevronRight else AllIconsKeys.General.ChevronDown,
                contentDescription = BetterLogcatBundle.message(if (isCollapsed) "problems.expand" else "problems.collapse"),
            )
        }
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A session: its title, when it started and how many problems it has. [isExpanded] null for the live one. */
@Composable
private fun SessionHeaderRow(
    session: ProblemSession,
    label: String?,
    isExpanded: Boolean?,
    colors: LogColors,
    onClick: () -> Unit,
    onMenu: (Point) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rememberContextClickModifier(onClick = onMenu))
            .hoverable(interactionSource = interactionSource)
            .background(color = if (isHovered && isExpanded != null) colors.hover else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                start = 8.dp,
                end = 12.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        if (isExpanded != null) {
            Icon(
                key = if (isExpanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
                contentDescription = BetterLogcatBundle.message(if (isExpanded) "problems.collapse" else "problems.expand"),
            )
        }
        Icon(
            key = AllIconsKeys.General.History,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        if (label != null) {
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = session.title,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = sessionTime(epochMillis = session.startedEpochMillis),
            color = colors.muted,
            maxLines = 1,
        )
        // Before the count, so counts line up with the problem rows' counts.
        if (onDelete != null) {
            ToolbarIconButton(
                modifier = Modifier
                    .size(20.dp)
                    .alpha(alpha = if (isHovered) 1f else 0f),
                iconKey = AllIconsKeys.General.Delete,
                tooltip = BetterLogcatBundle.message("problems.deleteSession"),
                onClick = onDelete,
            )
        }
        Text(
            text = session.problems.size.toString(),
            color = colors.muted,
        )
    }
}

/** One problem: icon, name and count, then the newest time, tag and detail. */
@Composable
private fun ProblemRow(
    problem: SavedProblem,
    colors: LogColors,
    indent: Dp,
    onClick: () -> Unit,
    onMenu: (Point) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Tooltip(tooltip = { Text(text = BetterLogcatBundle.message("problems.jump")) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(rememberContextClickModifier(onClick = onMenu))
                .hoverable(interactionSource = interactionSource)
                .background(color = if (isHovered) colors.hover else Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(
                    start = 8.dp + indent,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = 4.dp,
                ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        ) {
            Icon(
                key = problemIcon(kind = problem.kind),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(14.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
                ) {
                    Text(
                        text = problem.title,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (problem.kind == ProblemKind.WARNING) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = problem.count.toString(),
                        color = colors.muted,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
                ) {
                    Text(
                        text = Instant.ofEpochMilli(problem.lastEpochMillis).atZone(ZoneId.systemDefault()).format(CLOCK_FORMAT),
                        color = colors.muted,
                        style = JewelTheme.editorTextStyle,
                    )
                    Text(
                        text = problem.tag,
                        color = colors.tag(tag = problem.tag),
                        style = JewelTheme.editorTextStyle,
                        maxLines = 1,
                    )
                    if (problem.detail.isNotEmpty()) {
                        Text(
                            text = problem.detail,
                            modifier = Modifier.weight(1f),
                            color = colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private val CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

/**
 * Frame drops of the session: how often Choreographer skipped frames, how many, the worst one, and
 * bars of skipped frames per 10 seconds over the 5 minutes up to the newest drop.
 */
@Composable
private fun FrameDropsCard(
    skips: List<FrameSkip>,
    colors: LogColors,
) {
    val newest = skips.maxOf { it.epochMillis }
    val buckets = remember(skips) {
        IntArray(size = FRAME_BUCKETS).also { bars ->
            skips.forEach { skip ->
                val age = ((newest - skip.epochMillis) / FRAME_BUCKET_MILLIS).toInt()
                if (age in 0 until FRAME_BUCKETS) bars[FRAME_BUCKETS - 1 - age] += skip.frames
            }
        }
    }
    val peak = buckets.maxOrNull()?.coerceAtLeast(minimumValue = 1) ?: 1
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(
            text = BetterLogcatBundle.message(
                "problems.frames",
                skips.size,
                skips.sumOf { it.frames },
                skips.maxOf { it.frames },
            ),
            color = colors.muted,
        )
        val barColor = colors.level(level = LogLevel.WARN)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        ) {
            val barWidth = size.width / FRAME_BUCKETS
            buckets.forEachIndexed { index, frames ->
                if (frames == 0) return@forEachIndexed
                val barHeight = size.height * frames / peak
                drawRect(
                    color = barColor.copy(alpha = 0.75f),
                    topLeft = Offset(x = index * barWidth, y = size.height - barHeight),
                    size = Size(width = (barWidth - 2f).coerceAtLeast(minimumValue = 1f), height = barHeight),
                )
            }
        }
    }
}

// region Previews

private fun previewProblem(
    kind: ProblemKind,
    title: String,
    detail: String,
    tag: String,
    count: Int,
) = SavedProblem(
    key = kind.name + tag + title,
    kind = kind,
    title = title,
    detail = detail,
    tag = tag,
    count = count,
    firstEpochMillis = 1_791_540_000_000L,
    lastEpochMillis = 1_791_540_100_000L,
    level = LogLevel.ERROR,
    pid = 14_302,
    tid = 14_330,
    processName = "com.vintagegarage.app",
    sample = "$title: $detail",
)

private val previewProblems = ProblemsUiModel(
    current = ProblemSession(
        id = "live",
        title = "Google Pixel 9",
        startedEpochMillis = 1_791_540_000_000L,
        problems = listOf(
            previewProblem(kind = ProblemKind.CRASH, title = "IllegalStateException", detail = "Listing id must not be blank", tag = "AndroidRuntime", count = 1),
            previewProblem(kind = ProblemKind.ANR, title = "ANR in com.vintagegarage.app", detail = "Input dispatching timed out", tag = "ActivityManager", count = 1),
            previewProblem(kind = ProblemKind.EXCEPTION, title = "UnknownHostException", detail = "Unable to resolve host", tag = "OkHttp", count = 4),
            previewProblem(kind = ProblemKind.STRICT_MODE, title = "DiskReadViolation", detail = "~duration=48 ms", tag = "StrictMode", count = 2),
            previewProblem(kind = ProblemKind.WARNING, title = "Skipped 42 frames!", detail = "", tag = "Choreographer", count = 12),
        ),
    ),
    history = listOf(
        ProblemSession(
            id = "old",
            title = "com.vintagegarage.app launched · Google Pixel 9",
            startedEpochMillis = 1_791_450_000_000L,
            problems = listOf(previewProblem(kind = ProblemKind.EXCEPTION, title = "IOException", detail = "closed", tag = "Loader", count = 3)),
        ),
    ),
    frameSkips = listOf(
        FrameSkip(epochMillis = 1_791_540_000_000L, frames = 42),
        FrameSkip(epochMillis = 1_791_540_050_000L, frames = 120),
        FrameSkip(epochMillis = 1_791_540_100_000L, frames = 31),
    ),
)

@Preview
@Composable
private fun PreviewProblemsPanel() {
    BetterLogcatPreviewTheme {
        ProblemsPanel(
            modifier = Modifier.width(380.dp),
            problems = previewProblems,
            colors = rememberLogColors(),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewProblemsPanelEmpty() {
    BetterLogcatPreviewTheme {
        ProblemsPanel(
            modifier = Modifier.width(380.dp),
            problems = ProblemsUiModel(),
            colors = rememberLogColors(),
            onIntent = {},
        )
    }
}

// endregion
