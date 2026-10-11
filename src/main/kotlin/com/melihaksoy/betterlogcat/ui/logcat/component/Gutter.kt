package com.melihaksoy.betterlogcat.ui.logcat.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.ProblemKind
import com.melihaksoy.betterlogcat.network.NetworkCall
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.Point

/** Width of the gutter at the left of every row. */
internal val GUTTER = 18.dp

/** Smallest clickable height. Below one text line, so it never makes a row taller. */
private val GUTTER_HIT_HEIGHT = 14.dp

/** Gutter plus expand chevron: where the columns start. */
internal val LEADING_SLOT = GUTTER + EXPAND_SLOT

/**
 * What a row's gutter shows. [problem] is the crash, exception or warning in the row, and [call] the
 * network call this entry is part of, when there is one.
 */
@Immutable
data class GutterUiModel(
    val isBookmarked: Boolean = false,
    val problem: ProblemKind? = null,
    val call: NetworkCall? = null,
) {
    val isCrash: Boolean
        get() = problem == ProblemKind.CRASH

    /** Problem and network rows open a menu; other rows toggle a bookmark. */
    val hasMenu: Boolean
        get() = problem != null || call != null

    companion object {
        val None = GutterUiModel()
    }
}

/**
 * Left gutter: a crash, ANR, exception, StrictMode, network, bookmark or warning icon, in that order. A bookmark
 * shows over a warning, as warnings are many and the user set the bookmark. On hover an empty gutter
 * shows a faint bookmark, so it is clear that a click bookmarks the row.
 */
@Composable
internal fun Gutter(
    modifier: Modifier = Modifier,
    gutter: GutterUiModel,
    onClick: (Point) -> Unit,
) {
    val anchor = rememberSwingAnchor()
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = modifier
            .width(GUTTER)
            // An empty gutter has no content; without a height it could not be hovered or clicked.
            .heightIn(min = GUTTER_HIT_HEIGHT)
            .swingAnchor(anchor = anchor)
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick(anchor.bottomLeft(density = density)) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val problem = gutter.problem
        val key = when {
            problem != null && problem != ProblemKind.WARNING -> problemIcon(kind = problem)
            gutter.call != null -> AllIconsKeys.General.Web
            gutter.isBookmarked -> AllIconsKeys.Nodes.Bookmark
            problem != null -> problemIcon(kind = problem)
            isHovered -> AllIconsKeys.Nodes.Bookmark
            else -> null
        }
        val description = when {
            problem != null && problem != ProblemKind.WARNING -> problemDescriptionKey(kind = problem)
            gutter.isBookmarked -> "gutter.bookmarked"
            problem != null -> problemDescriptionKey(kind = problem)
            else -> "gutter.bookmark"
        }
        if (key != null) {
            Icon(
                key = key,
                contentDescription = BetterLogcatBundle.message(description),
                modifier = Modifier
                    .size(14.dp)
                    .alpha(alpha = if (isHovered && !gutter.isBookmarked && !gutter.hasMenu) 0.45f else 1f),
            )
        }
    }
}

private fun problemDescriptionKey(kind: ProblemKind): String = when (kind) {
    ProblemKind.CRASH -> "gutter.crash"
    ProblemKind.ANR -> "gutter.anr"
    ProblemKind.EXCEPTION -> "gutter.exception"
    ProblemKind.STRICT_MODE -> "gutter.strictMode"
    ProblemKind.WARNING -> "gutter.warning"
}

// region Previews

@Preview
@Composable
private fun PreviewGutterCrash() {
    BetterLogcatPreviewTheme {
        Gutter(
            gutter = GutterUiModel(problem = ProblemKind.CRASH),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewGutterException() {
    BetterLogcatPreviewTheme {
        Gutter(
            gutter = GutterUiModel(problem = ProblemKind.EXCEPTION),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewGutterWarning() {
    BetterLogcatPreviewTheme {
        Gutter(
            gutter = GutterUiModel(problem = ProblemKind.WARNING),
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewGutterBookmark() {
    BetterLogcatPreviewTheme {
        Gutter(
            gutter = GutterUiModel(isBookmarked = true),
            onClick = {},
        )
    }
}

// endregion
