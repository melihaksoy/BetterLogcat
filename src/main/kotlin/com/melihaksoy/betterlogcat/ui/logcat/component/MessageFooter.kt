package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.Text
import java.util.Locale

/**
 * Info and actions for a long, multi-line or JSON message, on their own strip under the message, so
 * they never read as part of the log text.
 */
@Composable
internal fun MessageFooter(
    modifier: Modifier = Modifier,
    entry: LogEntry,
    isExpanded: Boolean,
    colors: LogColors,
    onCopyJson: () -> Unit,
    onOpenInEditor: () -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val json = entry.json as? JsonContent.Found
    // Chips move to the next line in a narrow column; their text never breaks.
    FlowRow(
        modifier = modifier.padding(
            top = 2.dp,
            bottom = 2.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        val info = when {
            json != null -> BetterLogcatBundle.message("footer.json", sizeText(chars = json.main.raw.length))
            entry.lines.size > 1 -> BetterLogcatBundle.message("footer.lines", entry.lines.size, sizeText(chars = entry.message.length))
            else -> sizeText(chars = entry.message.length)
        }
        InfoChip(
            text = info,
            colors = colors,
        )
        if (json != null) {
            ActionChip(
                text = BetterLogcatBundle.message("row.copyJson"),
                colors = colors,
                onClick = onCopyJson,
            )
        }
        ActionChip(
            text = BetterLogcatBundle.message("row.open"),
            colors = colors,
            onClick = onOpenInEditor,
        )
        ActionChip(
            text = BetterLogcatBundle.message(if (isExpanded) "footer.collapse" else "footer.expand"),
            colors = colors,
            onClick = onToggleExpanded,
        )
    }
}

@Composable
private fun InfoChip(
    text: String,
    colors: LogColors,
) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                color = colors.muted.copy(alpha = 0.14f),
                shape = RoundedCornerShape(size = 4.dp),
            )
            .padding(
                horizontal = 6.dp,
                vertical = 1.dp,
            ),
        color = colors.muted,
        maxLines = 1,
        softWrap = false,
    )
}

@Composable
internal fun ActionChip(
    text: String,
    colors: LogColors,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Text(
        text = text,
        modifier = Modifier
            .hoverable(interactionSource = interactionSource)
            .background(
                color = colors.link.copy(alpha = if (isHovered) 0.28f else 0.14f),
                shape = RoundedCornerShape(size = 4.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = 6.dp,
                vertical = 1.dp,
            ),
        color = colors.link,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
    )
}

private fun sizeText(chars: Int): String = if (chars < 1_024) "$chars B" else String.format(Locale.ROOT, "%.1f KB", chars / 1_024.0)

// region Previews

@Preview
@Composable
private fun PreviewMessageFooterJson() {
    BetterLogcatPreviewTheme {
        MessageFooter(
            entry = LogEntry(
                id = 1,
                epochSeconds = 1_791_540_000L,
                nanos = 0,
                pid = 3_776,
                tid = 4_977,
                level = LogLevel.INFO,
                tag = "Ktor Client",
                message = "RESPONSE: 200 {\"results\":[{\"id\":25,\"name\":\"pikachu\"}]}",
            ),
            isExpanded = false,
            colors = rememberLogColors(),
            onCopyJson = {},
            onOpenInEditor = {},
            onToggleExpanded = {},
        )
    }
}

// endregion
