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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.format.MessageDocument
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.network.CurlBuilder
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.model.NetworkDetailUiModel
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticalScrollbar
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.datatransfer.StringSelection

/**
 * Request and response of one call, under the call list. The message is split into pieces in a lazy
 * list, so a big JSON body scrolls smoothly; text is selectable.
 */
@Composable
internal fun NetworkDetailPane(
    modifier: Modifier = Modifier,
    detail: NetworkDetailUiModel,
    colors: LogColors,
    keywords: KeywordHighlighter,
    onOpenInEditor: (LogEntry) -> Unit,
    onIntent: (LogcatIntent) -> Unit,
) {
    var showsResponse by remember(detail.call.requestEntryId) { mutableStateOf(value = detail.response != null) }
    val entry = if (showsResponse) detail.response else detail.request
    Column(modifier = modifier.background(color = JewelTheme.globalColors.panelBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 4.dp,
                    end = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab(
                text = BetterLogcatBundle.message("network.request"),
                isSelected = !showsResponse,
                colors = colors,
                onClick = { showsResponse = false },
            )
            Tab(
                text = if (detail.call.isPending) BetterLogcatBundle.message("network.waiting") else BetterLogcatBundle.message("network.response", detail.call.statusCode),
                isSelected = showsResponse,
                colors = colors,
                onClick = { showsResponse = true },
            )
            Spacer(modifier = Modifier.weight(1f))
            ToolbarIconButton(
                iconKey = AllIconsKeys.General.Close,
                tooltip = BetterLogcatBundle.message("network.detail.close"),
                onClick = { onIntent(LogcatIntent.SelectNetworkCall(call = null)) },
            )
        }
        Divider(orientation = Orientation.Horizontal)
        Text(
            text = "${detail.call.method} ${detail.call.url}",
            modifier = Modifier.padding(
                start = 8.dp,
                end = 8.dp,
                top = 6.dp,
            ),
            style = JewelTheme.editorTextStyle,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            if (entry != null) {
                Link(
                    text = BetterLogcatBundle.message("network.copy"),
                    onClick = { CopyPasteManager.getInstance().setContents(StringSelection(MessageDocument.text(entry = entry))) },
                )
                Link(
                    text = BetterLogcatBundle.message("menu.openInEditor"),
                    onClick = { onOpenInEditor(entry) },
                )
            }
            detail.request?.let { CurlBuilder.fromMessage(message = it.message) }?.let { curl ->
                Link(
                    text = BetterLogcatBundle.message("menu.copyCurl"),
                    onClick = { CopyPasteManager.getInstance().setContents(StringSelection(curl)) },
                )
            }
        }
        if (entry == null) {
            Text(
                text = BetterLogcatBundle.message(if (showsResponse) "network.noResponse" else "network.noRequest"),
                modifier = Modifier.padding(all = 8.dp),
                color = colors.muted,
            )
        } else {
            MessagePieces(
                modifier = Modifier.fillMaxSize(),
                entry = entry,
                colors = colors,
                keywords = keywords,
            )
        }
    }
}

@Composable
private fun MessagePieces(
    modifier: Modifier = Modifier,
    entry: LogEntry,
    colors: LogColors,
    keywords: KeywordHighlighter,
) {
    val textColor = JewelTheme.globalColors.text.normal
    val pieces = remember(entry, colors, keywords, textColor) {
        LogListLayout.split(
            text = buildMessageText(
                entry = entry,
                isExpanded = true,
                prettyJson = true,
                isFullyShown = true,
                textColor = textColor,
                colors = colors,
                highlighter = SearchHighlighter.None,
                keywords = keywords,
                labels = messageLabels(),
                onOpenFrame = {},
                onOpenUrl = { BrowserUtil.browse(it) },
                onShowAll = {},
            ),
            linesPerPiece = PIECE_LINES,
        )
    }
    val listState = rememberLazyListState()
    Box(modifier = modifier) {
        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                state = listState,
            ) {
                itemsIndexed(items = pieces) { _, piece ->
                    Text(
                        text = piece,
                        style = JewelTheme.editorTextStyle,
                    )
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

/** An IDE style tab: a hover background, a hand cursor, and a line under the selected one. */
@Composable
private fun Tab(
    text: String,
    isSelected: Boolean,
    colors: LogColors,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val underline = colors.link
    Text(
        text = text,
        modifier = Modifier
            .hoverable(interactionSource = interactionSource)
            .background(color = if (isHovered && !isSelected) colors.hover else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .pointerHoverIcon(icon = PointerIcon.Hand)
            .drawBehind {
                if (isSelected) {
                    val thickness = TAB_UNDERLINE.toPx()
                    drawRect(
                        color = underline,
                        topLeft = Offset(
                            x = 0f,
                            y = size.height - thickness,
                        ),
                        size = Size(
                            width = size.width,
                            height = thickness,
                        ),
                    )
                }
            }
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp,
            ),
        color = if (isSelected) JewelTheme.globalColors.text.normal else colors.muted,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
    )
}

private const val PIECE_LINES = 20
private val TAB_UNDERLINE = 2.dp
