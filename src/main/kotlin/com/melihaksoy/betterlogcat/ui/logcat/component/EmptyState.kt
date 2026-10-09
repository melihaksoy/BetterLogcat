package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.IconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys

/** Centered message for states without logs. Empty [actionLabel] hides the link. */
@Composable
internal fun EmptyState(
    modifier: Modifier = Modifier,
    iconKey: IconKey,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            key = iconKey,
            contentDescription = null,
        )
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (description.isNotEmpty()) {
            Text(
                text = description,
                modifier = Modifier.widthIn(max = 420.dp),
                color = JewelTheme.globalColors.text.info,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel.isNotEmpty()) {
            Link(
                text = actionLabel,
                onClick = onAction,
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun PreviewEmptyStateAdbMissing() {
    BetterLogcatPreviewTheme {
        EmptyState(
            iconKey = AllIconsKeys.General.Warning,
            title = BetterLogcatBundle.message("empty.adb.title"),
            description = BetterLogcatBundle.message("empty.adb.description"),
            actionLabel = BetterLogcatBundle.message("empty.adb.action"),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun PreviewEmptyStateNoDevices() {
    BetterLogcatPreviewTheme {
        EmptyState(
            iconKey = AllIconsKeys.General.Information,
            title = BetterLogcatBundle.message("empty.noDevices.title"),
            description = BetterLogcatBundle.message("empty.noDevices.description"),
            actionLabel = "",
            onAction = {},
        )
    }
}

// endregion
