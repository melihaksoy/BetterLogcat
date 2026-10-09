package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import com.melihaksoy.betterlogcat.ui.logcat.model.NetworkDetailUiModel
import com.melihaksoy.betterlogcat.model.LogEntry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
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
 * HTTP calls found in the logs (Ktor and OkHttp), newest first: status, method, path and time.
 * Click a call to jump to its response, or its request while it is still open.
 */
@Composable
internal fun NetworkPanel(
    modifier: Modifier = Modifier,
    calls: List<NetworkCall>,
    detail: NetworkDetailUiModel? = null,
    detailHeight: Int = 320,
    colors: LogColors,
    onOpenInEditor: (LogEntry) -> Unit = {},
    onIntent: (LogcatIntent) -> Unit,
) {
    val searchState = rememberTextFieldState()
    val query = searchState.text.toString().trim()
    val rows = remember(calls, query) {
        calls.asReversed().filter { query.isEmpty() || it.url.contains(other = query, ignoreCase = true) }
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
                text = BetterLogcatBundle.message("network.title", calls.size),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            ToolbarIconButton(
                iconKey = AllIconsKeys.General.Close,
                tooltip = BetterLogcatBundle.message("network.close"),
                onClick = { onIntent(LogcatIntent.ToggleSidePanel(panel = SidePanel.NETWORK)) },
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
            placeholder = { Text(text = BetterLogcatBundle.message("network.search")) },
            leadingIcon = {
                Icon(
                    key = AllIconsKeys.Actions.Search,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
            },
        )
        val listModifier = if (detail != null) Modifier.weight(1f) else Modifier
        if (rows.isEmpty()) {
            Text(
                text = BetterLogcatBundle.message(if (calls.isEmpty()) "network.empty" else "network.noMatch"),
                modifier = Modifier.padding(all = 10.dp),
                color = colors.muted,
            )
        } else {
            val listState = rememberLazyListState()
            Box(modifier = listModifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                ) {
                    items(
                        items = rows,
                        key = { "${it.requestEntryId}:${it.responseEntryId}:${it.requestEpochMillis}" },
                    ) { call ->
                        CallRow(
                            call = call,
                            colors = colors,
                            isSelected = detail?.call?.requestEntryId == call.requestEntryId && detail.call.requestEpochMillis == call.requestEpochMillis,
                            onClick = {
                                val target = if (call.isPending) call.requestEntryId else call.responseEntryId
                                onIntent(LogcatIntent.JumpToEntry(id = target))
                                onIntent(LogcatIntent.SelectNetworkCall(call = call))
                            },
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
        if (detail != null) {
            // Live height while dragging; saved when the drag ends.
            var liveHeight by remember(detailHeight) { mutableFloatStateOf(value = detailHeight.toFloat()) }
            ResizeHandle(
                isVertical = false,
                onDrag = { delta -> liveHeight = (liveHeight - delta).coerceIn(minimumValue = 120f, maximumValue = 1_200f) },
                onDragEnd = { onIntent(LogcatIntent.ResizeNetworkDetail(heightDp = liveHeight.toInt())) },
            )
            NetworkDetailPane(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(liveHeight.dp),
                detail = detail,
                colors = colors,
                onOpenInEditor = onOpenInEditor,
                onIntent = onIntent,
            )
        }
    }
}

@Composable
private fun CallRow(
    call: NetworkCall,
    isSelected: Boolean,
    colors: LogColors,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val statusColor = when {
        call.isPending -> colors.muted
        call.statusCode in 200..299 -> colors.info
        call.statusCode in 300..399 -> colors.link
        call.statusCode in 400..499 -> colors.warn
        else -> colors.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .background(
                color = when {
                    isSelected -> colors.link.copy(alpha = 0.16f)
                    isHovered -> colors.hover
                    else -> Color.Transparent
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = 8.dp,
                vertical = 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        Text(
            text = if (call.isPending) "…" else call.statusCode.toString(),
            modifier = Modifier
                .width(36.dp)
                .background(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(size = 3.dp),
                )
                .padding(horizontal = 4.dp),
            color = statusColor,
            style = JewelTheme.editorTextStyle,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = call.method.ifEmpty { "?" },
            modifier = Modifier.width(48.dp),
            style = JewelTheme.editorTextStyle,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = path(url = call.url),
            modifier = Modifier.weight(1f),
            style = JewelTheme.editorTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (call.durationMillis >= 0) {
            Text(
                text = BetterLogcatBundle.message("network.duration", call.durationMillis),
                color = colors.muted,
                style = JewelTheme.editorTextStyle,
            )
        }
    }
}

/** The address without scheme and host: `https://api.x.com/v1/items?id=2` → `/v1/items?id=2`. */
private fun path(url: String): String {
    val afterScheme = url.substringAfter("://", missingDelimiterValue = url)
    val slash = afterScheme.indexOf('/')
    return if (slash < 0) afterScheme else afterScheme.substring(slash)
}

// region Previews

@Preview
@Composable
private fun PreviewNetworkPanel() {
    BetterLogcatPreviewTheme {
        NetworkPanel(
            calls = listOf(
                NetworkCall(
                    method = "GET",
                    url = "https://api.vintagegarage.app/v1/search/top-picks",
                    requestEntryId = 1,
                    requestEpochMillis = 0,
                    responseEntryId = 2,
                    statusCode = 200,
                    durationMillis = 340,
                ),
                NetworkCall(
                    method = "POST",
                    url = "https://api.vintagegarage.app/v1/login",
                    requestEntryId = 3,
                    requestEpochMillis = 10,
                    responseEntryId = 4,
                    statusCode = 401,
                    durationMillis = 123,
                ),
                NetworkCall(
                    method = "GET",
                    url = "https://api.vintagegarage.app/v1/listings/88213",
                    requestEntryId = 5,
                    requestEpochMillis = 20,
                ),
            ),
            colors = rememberLogColors(),
            onIntent = {},
        )
    }
}

// endregion
