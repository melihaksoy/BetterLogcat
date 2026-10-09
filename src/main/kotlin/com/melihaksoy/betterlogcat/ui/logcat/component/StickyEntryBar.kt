package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.format.LogTimeFormatter
import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.TimeFormat
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/**
 * Pinned at the top while you scroll inside a big expanded message: says which entry you are in and
 * collapses it in one click.
 */
@Composable
internal fun StickyEntryBar(
    modifier: Modifier = Modifier,
    entry: LogEntry,
    colors: LogColors,
    textStyle: TextStyle,
    onCollapse: () -> Unit,
    onCopyJson: () -> Unit,
    onOpenInEditor: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp)
            .background(color = JewelTheme.globalColors.panelBackground)
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 10.dp),
    ) {
        ToolbarIconButton(
            iconKey = AllIconsKeys.General.ChevronUp,
            tooltip = BetterLogcatBundle.message("row.collapse"),
            onClick = onCollapse,
        )
        Text(
            text = LogTimeFormatter.format(
                entry = entry,
                format = TimeFormat.TIME,
                previous = null,
            ),
            color = colors.muted,
            style = textStyle,
        )
        Text(
            text = entry.tag,
            color = colors.tag(tag = entry.tag),
            style = textStyle,
        )
        Text(
            text = entry.lines.first(),
            modifier = Modifier.weight(1f),
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.json is JsonContent.Found) {
            Link(
                text = BetterLogcatBundle.message("row.copyJson"),
                onClick = onCopyJson,
            )
        }
        Link(
            text = BetterLogcatBundle.message("row.open"),
            onClick = onOpenInEditor,
        )
        Link(
            text = BetterLogcatBundle.message("row.collapse"),
            onClick = onCollapse,
        )
    }
}

// region Previews

@Preview
@Composable
private fun PreviewStickyEntryBar() {
    BetterLogcatPreviewTheme {
        StickyEntryBar(
            entry = LogEntry(
                id = 1,
                epochSeconds = 1_791_540_000L,
                nanos = 0,
                pid = 3_776,
                tid = 4_977,
                level = LogLevel.INFO,
                tag = "Ktor Client",
                message = "RESPONSE: 200\nMETHOD: GET",
            ),
            colors = rememberLogColors(),
            textStyle = JewelTheme.editorTextStyle,
            onCollapse = {},
            onCopyJson = {},
            onOpenInEditor = {},
        )
    }
}

// endregion
