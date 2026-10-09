package com.melihaksoy.betterlogcat.format

/** A web link in a message. [url] has JSON escapes removed (`https:\/\/x` → `https://x`). */
data class UrlMatch(
    val start: Int,
    val end: Int,
    val url: String,
)

object UrlFinder {

    private val urlRegex = Regex(pattern = """https?:(?:\\?/){2}[^\s"'<>`]+""")
    private const val TRAILING = ".,;:!?)]}"

    fun find(text: String): List<UrlMatch> {
        if (!text.contains("http")) return emptyList()
        return urlRegex.findAll(input = text).map { match ->
            val trimmed = match.value.trimEnd { it in TRAILING || it == '\\' }
            UrlMatch(
                start = match.range.first,
                end = match.range.first + trimmed.length,
                url = trimmed.replace("\\/", "/"),
            )
        }.toList()
    }
}
