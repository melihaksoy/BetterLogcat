package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.format.LogTimeFormatter
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.ProcessEvent
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/** A divider row for an app process start or end, so each run of the app is easy to tell apart. */
@Composable
internal fun ProcessMarkerRow(
    modifier: Modifier = Modifier,
    entry: LogEntry,
    event: ProcessEvent,
    displayName: String,
    isSelected: Boolean,
    colors: LogColors,
    textStyle: TextStyle,
) {
    val color = if (event is ProcessEvent.Started) colors.info else colors.error
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = if (isSelected) colors.link.copy(alpha = 0.16f) else Color.Transparent)
            .padding(
                horizontal = ROW_HORIZONTAL_PADDING,
                vertical = 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Line(
            modifier = Modifier.weight(1f),
            color = color,
        )
        Text(
            text = BetterLogcatBundle.message(
                if (event is ProcessEvent.Started) "process.started" else "process.died",
                displayName,
                event.pid,
                LogTimeFormatter.format(
                    entry = entry,
                    format = TimeFormat.TIME,
                    previous = null,
                ),
            ),
            color = color,
            style = textStyle,
            fontWeight = FontWeight.SemiBold,
        )
        Line(
            modifier = Modifier.weight(1f),
            color = color,
        )
    }
}

@Composable
private fun Line(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(color = color.copy(alpha = 0.5f)),
    )
}

// region Previews

@Preview
@Composable
private fun PreviewProcessStarted() {
    BetterLogcatPreviewTheme {
        ProcessMarkerRow(
            entry = LogEntry(
                id = 1,
                epochSeconds = 1_791_540_000L,
                nanos = 0,
                pid = 1_822,
                tid = 1_822,
                level = LogLevel.INFO,
                tag = "ActivityManager",
                message = "Start proc 15011:com.example.pokedex/u0a215 for next-top-activity",
            ),
            event = ProcessEvent.Started(
                packageName = "com.example.pokedex",
                pid = 15_011,
            ),
            displayName = "com.example.pokedex",
            isSelected = false,
            colors = rememberLogColors(),
            textStyle = JewelTheme.editorTextStyle,
        )
    }
}

// endregion
