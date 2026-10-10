package com.melihaksoy.betterlogcat.model

import androidx.compose.runtime.Immutable

/** One pattern of a preset. Its color comes from [role], never from the preset. Always a regex. */
@Immutable
data class PresetRule(
    val role: HighlightRole,
    val pattern: String,
)

/**
 * Ready-made highlights for the log format of a common library. A preset stays linked: it is turned
 * on or off as a whole, its roles can be turned off one by one, and role colors are shared with other
 * presets (see [PresetHighlights]). Patterns follow the library's current format, so a library
 * release that changes its format needs a plugin update. [id] is saved in settings; never change it.
 * [nameKey] is a message bundle key.
 */
@Immutable
data class HighlightPreset(
    val id: String,
    val nameKey: String,
    val rules: List<PresetRule>,
) {
    /** The roles in rule order, each once. One settings row per role. */
    val roles: List<HighlightRole> get() = rules.map { it.role }.distinct()

    fun patterns(role: HighlightRole): List<String> = rules.filter { it.role == role }.map { it.pattern }

    companion object {
        // The status code plus its reason phrase when there is one ("200 OK", "404 Not Found").
        private const val REASON = "(?: [A-Z][A-Za-z ]*[A-Za-z])?"

        /**
         * Ktor `Logging`: `REQUEST: <url>` or `RESPONSE: 200`, then `METHOD:`, `FROM:`, `COMMON HEADERS`,
         * `-> name: value` lines and `BODY START` ... `BODY END`.
         */
        val Ktor = HighlightPreset(
            id = "ktor",
            nameKey = "colors.presets.ktor",
            rules = listOf(
                PresetRule(
                    role = HighlightRole.HTTP_SECTION,
                    pattern = "(?m)^(?:REQUEST|RESPONSE|METHOD|FROM|BODY Content-Type)(?=:)|^(?:COMMON HEADERS|CONTENT HEADERS|BODY START|BODY END)$",
                ),
                PresetRule(
                    role = HighlightRole.HTTP_INFO,
                    pattern = "(?m)(?<=^RESPONSE: )[13]\\d\\d$REASON",
                ),
                PresetRule(
                    role = HighlightRole.HTTP_SUCCESS,
                    pattern = "(?m)(?<=^RESPONSE: )2\\d\\d$REASON",
                ),
                PresetRule(
                    role = HighlightRole.HTTP_CLIENT_ERROR,
                    pattern = "(?m)(?<=^RESPONSE: )4\\d\\d$REASON",
                ),
                PresetRule(
                    role = HighlightRole.HTTP_SERVER_ERROR,
                    pattern = "(?m)(?<=^RESPONSE: )5\\d\\d$REASON",
                ),
                // `GET`, and also the `HttpMethod(value=GET)` form.
                PresetRule(
                    role = HighlightRole.HTTP_METHOD,
                    pattern = "(?m)(?<=^METHOD: )(?:HttpMethod\\(value=[A-Z]+\\)|[A-Z]+)",
                ),
                PresetRule(
                    role = HighlightRole.HTTP_HEADER,
                    pattern = "(?m)(?<=^-> )[A-Za-z0-9-]+(?=:)",
                ),
            ),
        )

        val all: List<HighlightPreset> = listOf(Ktor)

        fun fromId(id: String): HighlightPreset? = all.firstOrNull { it.id == id }
    }
}
