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
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.logcat.LogcatState
import com.melihaksoy.betterlogcat.ui.logcat.model.LogcatContentUiModel
import com.melihaksoy.betterlogcat.ui.logcat.preview.LogcatPreviewData
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text

/** Bottom line: counts, selection, paused and connection state. */
@Composable
internal fun StatusLine(
    modifier: Modifier = Modifier,
    state: LogcatState,
    colors: LogColors,
    onIntent: (LogcatIntent) -> Unit = {},
) {
    val isConnected = (state.content as? LogcatContentUiModel.Logs)?.isDeviceConnected ?: true
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Text(
            text = BetterLogcatBundle.message("status.count", state.entries.size, state.totalCount),
            color = colors.muted,
        )
        if (state.selection.selectedIds.isNotEmpty()) {
            Text(
                text = BetterLogcatBundle.message("status.selected", state.selection.selectedIds.size),
                color = colors.muted,
            )
        }
        if (state.bookmarkedIds.isNotEmpty()) {
            Text(
                text = BetterLogcatBundle.message("status.bookmarks", state.bookmarkedIds.size),
                color = colors.muted,
            )
            Link(
                text = "↑",
                onClick = { onIntent(LogcatIntent.JumpToBookmark(delta = -1)) },
            )
            Link(
                text = "↓",
                onClick = { onIntent(LogcatIntent.JumpToBookmark(delta = 1)) },
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (state.crashCount > 0) {
            // A crash badge that shows on every tab and package; one click jumps to the latest crash.
            Link(
                text = BetterLogcatBundle.message("status.crashes", state.crashCount),
                onClick = { onIntent(LogcatIntent.JumpToCrash) },
            )
        }
        if (state.isPaused) {
            Text(
                text = BetterLogcatBundle.message("status.paused", state.pausedNewCount),
                color = colors.warn,
            )
        }
        if (!isConnected) {
            Text(
                text = BetterLogcatBundle.message("status.disconnected"),
                color = colors.error,
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun PreviewStatusLine() {
    BetterLogcatPreviewTheme {
        StatusLine(
            state = LogcatPreviewData.logsState,
            colors = rememberLogColors(),
        )
    }
}

@Preview
@Composable
private fun PreviewStatusLinePausedDisconnected() {
    BetterLogcatPreviewTheme {
        StatusLine(
            state = LogcatPreviewData.logsState.copy(
                isPaused = true,
                pausedNewCount = 318,
                content = LogcatContentUiModel.Logs(isDeviceConnected = false),
            ),
            colors = rememberLogColors(),
        )
    }
}

// endregion
