package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/** Shown while the pane shows a saved log file instead of the device: which file, and the way back. */
@Composable
internal fun OpenedFileBar(
    modifier: Modifier = Modifier,
    fileName: String,
    colors: LogColors,
    onBackToLive: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = colors.headerBackground)
            .padding(
                horizontal = 10.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Icon(
            key = AllIconsKeys.FileTypes.Text,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = BetterLogcatBundle.message("file.viewing", fileName),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Link(
            text = BetterLogcatBundle.message("file.backToLive"),
            onClick = onBackToLive,
        )
    }
}

// region Previews

@Preview
@Composable
private fun PreviewOpenedFileBar() {
    BetterLogcatPreviewTheme {
        OpenedFileBar(
            fileName = "logcat-Pixel-9-20261011-222414.txt",
            colors = rememberLogColors(),
            onBackToLive = {},
        )
    }
}

// endregion
