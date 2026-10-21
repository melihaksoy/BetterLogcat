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
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

/** A divider under the last line loaded from the previous IDE session. Live lines follow below it. */
@Composable
internal fun PreviousLogMarkerRow(
    modifier: Modifier = Modifier,
    entry: LogEntry,
    deviceName: String,
    colors: LogColors,
    textStyle: TextStyle,
) {
    // The previous session can be days old, so the date shows too.
    val time = LogTimeFormatter.format(
        entry = entry,
        format = TimeFormat.DATE_TIME,
        previous = null,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ROW_HORIZONTAL_PADDING,
                vertical = 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Line(
            modifier = Modifier.weight(1f),
            color = colors.muted,
        )
        Text(
            text = if (deviceName.isEmpty()) {
                BetterLogcatBundle.message("previousLog.end", time)
            } else {
                BetterLogcatBundle.message("previousLog.endOfDevice", deviceName, time)
            },
            color = colors.muted,
            style = textStyle,
            fontWeight = FontWeight.SemiBold,
        )
        Line(
            modifier = Modifier.weight(1f),
            color = colors.muted,
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
private fun PreviewPreviousLogMarkerRow() {
    BetterLogcatPreviewTheme {
        PreviousLogMarkerRow(
            entry = LogEntry(
                id = 1,
                epochSeconds = 1_791_540_000L,
                nanos = 0,
                pid = 15_011,
                tid = 15_011,
                level = LogLevel.INFO,
                tag = "PokedexRepository",
                message = "Loaded 151 entries",
            ),
            deviceName = "Pixel 9 API 36",
            colors = rememberLogColors(),
            textStyle = JewelTheme.editorTextStyle,
        )
    }
}

// endregion
