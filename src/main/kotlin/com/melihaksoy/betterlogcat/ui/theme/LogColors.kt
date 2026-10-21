package com.melihaksoy.betterlogcat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.melihaksoy.betterlogcat.format.JsonTokenKind
import com.melihaksoy.betterlogcat.model.LogLevel
import org.jetbrains.jewel.foundation.theme.JewelTheme
import kotlin.math.absoluteValue

@Immutable
data class LogColors(
    val verbose: Color,
    val debug: Color,
    val info: Color,
    val warn: Color,
    val error: Color,
    val assert: Color,
    val tagPalette: List<Color>,
    val selection: Color,
    val hover: Color,
    val searchHighlight: Color,
    val link: Color,
    val jsonKey: Color,
    val jsonString: Color,
    val jsonNumber: Color,
    val jsonKeyword: Color,
    val muted: Color,
    val headerBackground: Color,
    /** Background of a cell in text selection mode, like an editor field. */
    val fieldBackground: Color,
    val textSelection: Color,
    /** Levels the user gave a color. Their messages use that color too, not the theme text color. */
    val overriddenLevels: Set<LogLevel> = emptySet(),
) {
    fun level(level: LogLevel): Color = when (level) {
        LogLevel.VERBOSE -> verbose
        LogLevel.DEBUG -> debug
        LogLevel.INFO -> info
        LogLevel.WARN -> warn
        LogLevel.ERROR -> error
        LogLevel.ASSERT -> assert
    }

    /** 2xx info, 3xx link, 4xx warn, else (5xx, a failed call) error. */
    fun httpStatus(statusCode: Int): Color = when (statusCode) {
        in 200..299 -> info
        in 300..399 -> link
        in 400..499 -> warn
        else -> error
    }

    /** This palette with the user's level colors on top. */
    fun withLevelColors(overrides: Map<LogLevel, Long>): LogColors {
        if (overrides.isEmpty()) return this
        fun pick(level: LogLevel, default: Color) = overrides[level]?.let { Color(it) } ?: default
        return copy(
            verbose = pick(level = LogLevel.VERBOSE, default = verbose),
            debug = pick(level = LogLevel.DEBUG, default = debug),
            info = pick(level = LogLevel.INFO, default = info),
            warn = pick(level = LogLevel.WARN, default = warn),
            error = pick(level = LogLevel.ERROR, default = error),
            assert = pick(level = LogLevel.ASSERT, default = assert),
            overriddenLevels = overrides.keys,
        )
    }

    fun tag(tag: String): Color = tagPalette[tag.hashCode().absoluteValue % tagPalette.size]

    fun json(kind: JsonTokenKind): Color = when (kind) {
        JsonTokenKind.KEY -> jsonKey
        JsonTokenKind.STRING -> jsonString
        JsonTokenKind.NUMBER -> jsonNumber
        JsonTokenKind.KEYWORD -> jsonKeyword
    }

    companion object {
        val Dark = LogColors(
            verbose = Color(0xFF9DA0A8),
            debug = Color(0xFF5FB0E8),
            info = Color(0xFF6CC18B),
            warn = Color(0xFFE8B04B),
            error = Color(0xFFF2685F),
            assert = Color(0xFFE05BD0),
            tagPalette = listOf(
                Color(0xFF7AA2F7),
                Color(0xFF9ECE6A),
                Color(0xFFE0AF68),
                Color(0xFFBB9AF7),
                Color(0xFF7DCFFF),
                Color(0xFFFF9E64),
                Color(0xFF73DACA),
                Color(0xFFF7768E),
            ),
            selection = Color(0xFF2E436E),
            hover = Color(0x0FFFFFFF),
            searchHighlight = Color(0x66C9A227),
            link = Color(0xFF548AF7),
            jsonKey = Color(0xFFC77DBB),
            jsonString = Color(0xFF6AAB73),
            jsonNumber = Color(0xFF2AACB8),
            jsonKeyword = Color(0xFFCF8E6D),
            muted = Color(0xFF868A91),
            headerBackground = Color(0x0AFFFFFF),
            fieldBackground = Color(0xFF1E1F22),
            textSelection = Color(0xFF214283),
        )

        val Light = LogColors(
            verbose = Color(0xFF6C707E),
            debug = Color(0xFF2771C9),
            info = Color(0xFF208A3C),
            warn = Color(0xFFA86B00),
            error = Color(0xFFD32F2F),
            assert = Color(0xFFB0249A),
            tagPalette = listOf(
                Color(0xFF2F65CA),
                Color(0xFF3D7A1F),
                Color(0xFF9A6300),
                Color(0xFF7A3EC8),
                Color(0xFF00788A),
                Color(0xFFB4501E),
                Color(0xFF1B7F6A),
                Color(0xFFC0304F),
            ),
            selection = Color(0xFFD5E1FF),
            hover = Color(0x0A000000),
            searchHighlight = Color(0x80FFD54F),
            link = Color(0xFF2F65CA),
            jsonKey = Color(0xFF871094),
            jsonString = Color(0xFF067D17),
            jsonNumber = Color(0xFF1750EB),
            jsonKeyword = Color(0xFF0033B3),
            muted = Color(0xFF818594),
            headerBackground = Color(0x08000000),
            fieldBackground = Color(0xFFFFFFFF),
            textSelection = Color(0xFFA6D2FF),
        )
    }
}

@Composable
fun rememberLogColors(): LogColors = if (JewelTheme.isDark) LogColors.Dark else LogColors.Light
