package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable

/**
 * Ready-made keyword highlights for the log format of a common library. Adding one adds its [rules]
 * as normal keyword highlights, so each can be recolored or removed on its own. [nameKey] is a
 * message bundle key.
 */
@Immutable
data class HighlightPreset(
    val nameKey: String,
    val rules: List<HighlightRule>,
) {
    /** True when every rule of the preset is in [current]. Matched by pattern, so recolored rules count. */
    fun isAdded(current: List<HighlightRule>): Boolean {
        val patterns = current.mapTo(destination = HashSet()) { it.pattern }
        return rules.all { it.pattern in patterns }
    }

    companion object {
        private const val GREEN = 0xFF2EB67D
        private const val BLUE = 0xFF61AFEF
        private const val ORANGE = 0xFFF39C12
        private const val RED = 0xFFE06C75
        private const val CYAN = 0xFF56B6C2
        private const val PURPLE = 0xFFC678DD
        private const val INDIGO = 0xFF7C83FD

        // The status code plus its reason phrase when there is one ("200 OK", "404 Not Found").
        private const val REASON = "(?: [A-Z][A-Za-z ]*[A-Za-z])?"

        /**
         * Ktor `Logging`: `REQUEST: <url>` or `RESPONSE: 200`, then `METHOD:`, `FROM:`, `COMMON HEADERS`,
         * `-> name: value` lines and `BODY START` ... `BODY END`.
         */
        val Ktor = HighlightPreset(
            nameKey = "colors.presets.ktor",
            rules = listOf(
                regex(
                    pattern = "(?m)^(?:REQUEST|RESPONSE|METHOD|FROM|BODY Content-Type)(?=:)|^(?:COMMON HEADERS|CONTENT HEADERS|BODY START|BODY END)$",
                    colorArgb = PURPLE,
                ),
                regex(
                    pattern = "(?m)(?<=^RESPONSE: )[13]\\d\\d$REASON",
                    colorArgb = BLUE,
                ),
                regex(
                    pattern = "(?m)(?<=^RESPONSE: )2\\d\\d$REASON",
                    colorArgb = GREEN,
                ),
                regex(
                    pattern = "(?m)(?<=^RESPONSE: )4\\d\\d$REASON",
                    colorArgb = ORANGE,
                ),
                regex(
                    pattern = "(?m)(?<=^RESPONSE: )5\\d\\d$REASON",
                    colorArgb = RED,
                ),
                // `GET`, and also the `HttpMethod(value=GET)` form.
                regex(
                    pattern = "(?m)(?<=^METHOD: )(?:HttpMethod\\(value=[A-Z]+\\)|[A-Z]+)",
                    colorArgb = CYAN,
                ),
                regex(
                    pattern = "(?m)(?<=^-> )[A-Za-z0-9-]+(?=:)",
                    colorArgb = INDIGO,
                ),
            ),
        )

        val all: List<HighlightPreset> = listOf(Ktor)

        private fun regex(
            pattern: String,
            colorArgb: Long,
        ) = HighlightRule(
            pattern = pattern,
            colorArgb = colorArgb,
            isRegex = true,
        )
    }
}
