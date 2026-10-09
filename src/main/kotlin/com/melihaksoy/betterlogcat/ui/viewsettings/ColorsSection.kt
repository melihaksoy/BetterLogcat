package com.melihaksoy.betterlogcat.ui.viewsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melihaksoy.betterlogcat.BetterLogcatBundle
import com.melihaksoy.betterlogcat.model.HighlightRule
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.ui.logcat.LogcatIntent
import com.melihaksoy.betterlogcat.ui.theme.BetterLogcatPreviewTheme
import com.melihaksoy.betterlogcat.ui.theme.LogColors
import com.melihaksoy.betterlogcat.ui.theme.rememberLogColors
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.GroupHeader
import org.jetbrains.jewel.ui.component.Link
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

/** Colors that read well on light and dark themes. */
private val PALETTE = listOf(
    0xFFE06C75,
    0xFFF39C12,
    0xFFE5C07B,
    0xFF98C379,
    0xFF2EB67D,
    0xFF56B6C2,
    0xFF61AFEF,
    0xFF7C83FD,
    0xFFC678DD,
    0xFFFF79C6,
    0xFFB0B0B0,
)

private val SWATCH_SIZE = 16.dp

/** Level color overrides and keyword highlights. */
@Composable
internal fun ColorsSection(
    levelColors: Map<LogLevel, Long>,
    highlightRules: List<HighlightRule>,
    colors: LogColors,
    onIntent: (LogcatIntent) -> Unit,
    onPickColor: (initial: Long, onPicked: (Long) -> Unit) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        GroupHeader(text = BetterLogcatBundle.message("colors.levels"))
        LogLevel.entries.forEach { level ->
            LevelColorRow(
                level = level,
                selected = levelColors[level],
                colors = colors,
                onPick = { argb ->
                    onIntent(
                        LogcatIntent.SetLevelColor(
                            level = level,
                            colorArgb = argb,
                        ),
                    )
                },
                onPickColor = onPickColor,
            )
        }
        GroupHeader(
            text = BetterLogcatBundle.message("colors.keywords"),
            modifier = Modifier.padding(top = 6.dp),
        )
        if (highlightRules.isEmpty()) {
            Text(
                text = BetterLogcatBundle.message("colors.keywords.empty"),
                color = colors.muted,
            )
        }
        // Its own scroll box, so a long keyword list never pushes the other settings away.
        if (highlightRules.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .heightIn(max = KEYWORD_LIST_HEIGHT)
                    .border(
                        width = 1.dp,
                        color = JewelTheme.globalColors.borders.normal,
                        shape = RoundedCornerShape(size = 4.dp),
                    )
                    .verticalScroll(state = rememberScrollState())
                    .padding(all = 6.dp),
                verticalArrangement = Arrangement.spacedBy(space = 4.dp),
            ) {
        highlightRules.forEach { rule ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Swatch(
                    color = Color(rule.colorArgb),
                    isSelected = false,
                    onClick = {
                        onPickColor(rule.colorArgb) { argb -> onIntent(LogcatIntent.AddHighlightRule(rule = rule.copy(colorArgb = argb))) }
                    },
                )
                Text(
                    text = rule.pattern,
                    color = Color(rule.colorArgb),
                    style = JewelTheme.editorTextStyle,
                    fontWeight = FontWeight.SemiBold,
                )
                if (rule.isRegex) {
                    Text(
                        text = BetterLogcatBundle.message("colors.keywords.regex"),
                        color = colors.muted,
                    )
                }
                Link(
                    text = BetterLogcatBundle.message("aliases.remove"),
                    onClick = { onIntent(LogcatIntent.RemoveHighlightRule(rule = rule)) },
                )
            }
        }
            }
        }
        AddKeywordRow(
            colors = colors,
            onAdd = { onIntent(LogcatIntent.AddHighlightRule(rule = it)) },
            onPickColor = onPickColor,
        )
    }
}

@Composable
private fun LevelColorRow(
    level: LogLevel,
    selected: Long?,
    colors: LogColors,
    onPick: (Long?) -> Unit,
    onPickColor: (initial: Long, onPicked: (Long) -> Unit) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(
            text = level.letter.toString(),
            modifier = Modifier.width(18.dp),
            color = colors.level(level = level),
            fontWeight = FontWeight.Bold,
        )
        PALETTE.forEach { argb ->
            Swatch(
                color = Color(argb),
                isSelected = argb == selected,
                onClick = { onPick(argb) },
            )
        }
        CustomSwatch(
            custom = selected?.takeIf { it !in PALETTE },
            onClick = { onPickColor(selected ?: colors.level(level = level).value.toArgbLong()) { onPick(it) } },
        )
        if (selected != null) {
            Link(
                text = BetterLogcatBundle.message("colors.reset"),
                onClick = { onPick(null) },
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun AddKeywordRow(
    colors: LogColors,
    onAdd: (HighlightRule) -> Unit,
    onPickColor: (initial: Long, onPicked: (Long) -> Unit) -> Unit,
) {
    val patternState = rememberTextFieldState()
    var isRegex by remember { mutableStateOf(value = false) }
    var colorArgb by remember { mutableLongStateOf(value = PALETTE.first()) }
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            TextField(
                state = patternState,
                modifier = Modifier.width(180.dp),
                placeholder = { Text(text = BetterLogcatBundle.message("colors.keywords.placeholder")) },
            )
            CheckboxRow(
                text = BetterLogcatBundle.message("colors.keywords.regex"),
                checked = isRegex,
                onCheckedChange = { isRegex = it },
            )
            OutlinedButton(
                onClick = {
                    val pattern = patternState.text.toString().trim()
                    if (pattern.isNotEmpty()) {
                        onAdd(
                            HighlightRule(
                                pattern = pattern,
                                colorArgb = colorArgb,
                                isRegex = isRegex,
                            ),
                        )
                        patternState.clearText()
                    }
                },
            ) {
                Text(text = BetterLogcatBundle.message("colors.keywords.add"))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(space = 4.dp)) {
            PALETTE.forEach { argb ->
                Swatch(
                    color = Color(argb),
                    isSelected = argb == colorArgb,
                    onClick = { colorArgb = argb },
                )
            }
            CustomSwatch(
                custom = colorArgb.takeIf { it !in PALETTE },
                onClick = { onPickColor(colorArgb) { colorArgb = it } },
            )
        }
        Text(
            text = BetterLogcatBundle.message("colors.keywords.hint"),
            color = colors.muted,
        )
    }
}

/** "+" opens the IDE color picker. Shows the picked color once there is a custom one. */
@Composable
private fun CustomSwatch(
    custom: Long?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(size = 4.dp)
    Box(
        modifier = Modifier
            .size(SWATCH_SIZE + 4.dp)
            .border(
                width = if (custom != null) 2.dp else 1.dp,
                color = if (custom != null) JewelTheme.globalColors.text.normal else JewelTheme.globalColors.borders.normal,
                shape = shape,
            )
            .padding(all = 2.dp)
            .background(
                color = custom?.let { Color(it) } ?: Color.Transparent,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (custom == null) {
            Text(
                text = "+",
                color = JewelTheme.globalColors.text.info,
            )
        }
    }
}

private fun ULong.toArgbLong(): Long = (this shr 32).toLong() and 0xFFFFFFFFL

private val KEYWORD_LIST_HEIGHT = 180.dp

@Composable
private fun Swatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(size = 4.dp)
    Box(
        modifier = Modifier
            .size(SWATCH_SIZE + 4.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) JewelTheme.globalColors.text.normal else Color.Transparent,
                shape = shape,
            )
            .padding(all = 2.dp)
            .background(
                color = color,
                shape = shape,
            )
            .clickable(onClick = onClick),
    )
}

// region Previews

@Preview
@Composable
private fun PreviewColorsSection() {
    BetterLogcatPreviewTheme {
        ColorsSection(
            levelColors = mapOf(LogLevel.DEBUG to 0xFF56B6C2),
            highlightRules = listOf(
                HighlightRule(
                    pattern = "timeout",
                    colorArgb = 0xFFE06C75,
                    isRegex = false,
                ),
                HighlightRule(
                    pattern = "user_\\d+",
                    colorArgb = 0xFFC678DD,
                    isRegex = true,
                ),
            ),
            colors = rememberLogColors(),
            onIntent = {},
            onPickColor = { _, _ -> },
        )
    }
}

// endregion
