package com.melihaksoy.betterlogcat.ui.logcat.component

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.format.LogTimeFormatter
import com.melihaksoy.betterlogcat.model.Bookmark
import com.melihaksoy.betterlogcat.model.BookmarkKey
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.model.BookmarkUiModel
import com.melihaksoy.betterlogcat.ui.logcat.model.SidePanel
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Bookmarked lines of the project, in log order. Click one to jump to it. A line that left the log
 * (Clear, trim, restart) is still saved: click it to open the saved copy in an editor.
 */
@Composable
internal fun BookmarksPanel(
    modifier: Modifier = Modifier,
    bookmarks: List<BookmarkUiModel>,
    colors: LogColors,
    keywords: KeywordHighlighter = KeywordHighlighter.None,
    onOpenInEditor: (LogEntry) -> Unit,
    onIntent: (LogcatIntent) -> Unit,
) {
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString().trim()
    val rows = remember(bookmarks, query) {
        bookmarks.filter {
            query.isEmpty() ||
                it.bookmark.key.tag.contains(other = query, ignoreCase = true) ||
                it.bookmark.message.contains(other = query, ignoreCase = true)
        }
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
                text = BetterLogcatBundle.message("bookmarks.title", bookmarks.size),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            ToolbarIconButton(
                iconKey = AllIconsKeys.General.Close,
                tooltip = BetterLogcatBundle.message("bookmarks.close"),
                onClick = { onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.BOOKMARKS)) },
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
            placeholder = { Text(text = BetterLogcatBundle.message("bookmarks.search")) },
            leadingIcon = {
                Icon(
                    key = AllIconsKeys.Actions.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
            },
        )
        if (rows.isEmpty()) {
            Text(
                text = BetterLogcatBundle.message(if (bookmarks.isEmpty()) "bookmarks.empty" else "bookmarks.noMatch"),
                modifier = Modifier.padding(all = 10.dp),
                color = colors.muted,
            )
        } else {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                ) {
                    items(
                        items = rows,
                        key = { it.bookmark.key.toString() },
                    ) { row ->
                        BookmarkRow(
                            row = row,
                            colors = colors,
                            keywords = keywords,
                            onClick = {
                                if (row.isInLog) {
                                    onIntent(LogcatIntent.JumpToEntry(id = row.liveId))
                                } else {
                                    onOpenInEditor(row.bookmark.toLogEntry(id = row.liveId))
                                }
                            },
                            onOpenInEditor = { onOpenInEditor(row.bookmark.toLogEntry(id = row.liveId)) },
                            onRemove = { onIntent(LogcatIntent.RemoveBookmark(key = row.bookmark.key)) },
                        )
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
}

@Composable
private fun BookmarkRow(
    row: BookmarkUiModel,
    colors: LogColors,
    keywords: KeywordHighlighter,
    onClick: () -> Unit,
    onOpenInEditor: () -> Unit,
    onRemove: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bookmark = row.bookmark
    val entry = remember(bookmark) { bookmark.toLogEntry(id = row.liveId) }
    val textColor = JewelTheme.globalColors.text.normal
    val message = remember(bookmark, keywords, textColor) {
        val line = bookmark.message.lineSequence().firstOrNull().orEmpty()
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = textColor)) { append(line) }
            keywords.apply(
                builder = this,
                text = line,
                offset = 0,
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .background(color = if (isHovered) colors.hover else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                start = 8.dp,
                end = 4.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        LevelBadge(
            level = bookmark.level,
            colors = colors,
            textStyle = JewelTheme.editorTextStyle,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(alpha = if (row.isInLog) 1f else 0.6f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
            ) {
                Text(
                    text = LogTimeFormatter.format(
                        entry = entry,
                        format = TimeFormat.TIME,
                        previous = null,
                    ),
                    color = colors.muted,
                    style = JewelTheme.editorTextStyle,
                )
                Text(
                    text = bookmark.key.tag,
                    modifier = Modifier.weight(
                        weight = 1f,
                        fill = false,
                    ),
                    color = colors.tag(tag = bookmark.key.tag),
                    style = JewelTheme.editorTextStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!row.isInLog) {
                    Text(
                        text = BetterLogcatBundle.message("bookmarks.notInLog"),
                        color = colors.muted,
                    )
                }
            }
            Text(
                text = message,
                style = JewelTheme.editorTextStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(modifier = Modifier.alpha(alpha = if (isHovered) 1f else 0f)) {
            ToolbarIconButton(
                modifier = Modifier.size(22.dp),
                iconKey = AllIconsKeys.Actions.MoveTo2,
                tooltip = BetterLogcatBundle.message("bookmarks.open"),
                onClick = onOpenInEditor,
            )
            ToolbarIconButton(
                modifier = Modifier.size(22.dp),
                iconKey = AllIconsKeys.General.CloseSmall,
                tooltip = BetterLogcatBundle.message("bookmarks.remove"),
                onClick = onRemove,
            )
        }
    }
}

// region Previews

private fun previewBookmark(
    seconds: Long,
    level: LogLevel,
    tag: String,
    message: String,
) = Bookmark(
    key = BookmarkKey(
        epochSeconds = 1_791_540_000L + seconds,
        nanos = 120_000_000,
        pid = 14_302,
        tid = 14_330,
        tag = tag,
    ),
    level = level,
    message = message,
    processName = "com.example.pokedex",
    deviceName = "Google Pixel 9",
)

@Preview
@Composable
private fun PreviewBookmarksPanel() {
    BetterLogcatPreviewTheme {
        BookmarksPanel(
            modifier = Modifier.width(360.dp),
            bookmarks = listOf(
                BookmarkUiModel(
                    bookmark = previewBookmark(
                        seconds = 0,
                        level = LogLevel.ERROR,
                        tag = "AndroidRuntime",
                        message = "FATAL EXCEPTION: main\nProcess: com.example.pokedex, PID: 14302",
                    ),
                ),
                BookmarkUiModel(
                    bookmark = previewBookmark(
                        seconds = 42,
                        level = LogLevel.INFO,
                        tag = "Ktor Client",
                        message = "RESPONSE: 200 OK\nFROM: https://pokeapi.co/api/v2/pokemon/25",
                    ),
                    liveId = 7,
                ),
            ),
            colors = rememberLogColors(),
            onOpenInEditor = {},
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun PreviewBookmarksPanelEmpty() {
    BetterLogcatPreviewTheme {
        BookmarksPanel(
            modifier = Modifier.width(360.dp),
            bookmarks = emptyList(),
            colors = rememberLogColors(),
            onOpenInEditor = {},
            onIntent = {},
        )
    }
}

// endregion
