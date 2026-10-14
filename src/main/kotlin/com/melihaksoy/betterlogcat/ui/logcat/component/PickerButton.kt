package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.Point

/** Combo-box style button that opens a native IDE chooser under itself. */
@Composable
internal fun PickerButton(
    modifier: Modifier = Modifier,
    label: String,
    detail: String,
    statusColor: Color,
    onClick: (Point) -> Unit,
) {
    val anchor = rememberSwingAnchor()
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(size = 4.dp)
    Row(
        modifier = modifier
            .swingAnchor(anchor = anchor)
            .height(26.dp)
            .widthIn(max = 320.dp)
            .hoverable(interactionSource = interactionSource)
            .background(
                color = if (isHovered) JewelTheme.globalColors.text.normal.copy(alpha = 0.06f) else Color.Transparent,
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = JewelTheme.globalColors.borders.normal,
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick(anchor.bottomLeft(density = density)) },
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
    ) {
        if (statusColor != Color.Unspecified) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = statusColor,
                        shape = CircleShape,
                    ),
            )
        }
        val detailColor = JewelTheme.globalColors.text.info
        // One text, so the end (the detail) is cut first and the label stays readable.
        val text = remember(label, detail, detailColor) {
            buildAnnotatedString {
                append(label)
                if (detail.isNotEmpty()) {
                    append("  ")
                    withStyle(style = SpanStyle(color = detailColor)) { append(detail) }
                }
            }
        }
        Text(
            text = text,
            modifier = Modifier.weight(
                weight = 1f,
                fill = false,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            key = AllIconsKeys.General.ChevronDown,
            contentDescription = null,
        )
    }
}

// region Previews

@Preview
@Composable
private fun PreviewPickerButtonDevice() {
    BetterLogcatPreviewTheme {
        PickerButton(
            label = "Google Pixel 9",
            detail = "API 36",
            statusColor = Color(0xFF5FB865),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewPickerButtonPackage() {
    BetterLogcatPreviewTheme {
        PickerButton(
            label = "com.example.pokedex",
            detail = "",
            statusColor = Color.Unspecified,
            onClick = {},
        )
    }
}

// endregion
