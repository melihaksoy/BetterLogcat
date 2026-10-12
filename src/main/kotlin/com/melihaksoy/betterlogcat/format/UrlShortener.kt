package com.melihaksoy.betterlogcat.format

import androidx.compose.runtime.Immutable

/** Text with web links found. [links] have offsets in [text] and the full address in [UrlMatch.url]. */
data class LinkedText(
    val text: String,
    val links: List<UrlMatch>,
)

/**
 * Hides the user's base URLs in log text: `https://staging.example.com/app-api/v1/items` shows as
 * `/app-api/v1/items`. The link keeps the full address. A base without a scheme matches any scheme.
 * A data class on the bases, so rows only rebuild text when they change.
 */
@Immutable
data class UrlShortener(val bases: List<String>) {

    /** Longest first, so `https://x.com/api` wins over `https://x.com`. */
    private val sortedBases by lazy(LazyThreadSafetyMode.NONE) {
        bases.map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }.distinct().sortedByDescending { it.length }
    }

    fun shorten(text: String): LinkedText {
        val matches = UrlFinder.find(text = text)
        if (sortedBases.isEmpty() || matches.isEmpty()) {
            return LinkedText(
                text = text,
                links = matches,
            )
        }
        val shown = StringBuilder(text.length)
        val links = ArrayList<UrlMatch>(matches.size)
        var cursor = 0
        matches.forEach { match ->
            shown.append(text, cursor, match.start)
            val hidden = hiddenLength(url = match.url)
            // Raw characters of the hidden part. JSON writes `/` as `\/`, so raw text can be longer than the address.
            val rawHidden = rawLength(
                raw = text,
                start = match.start,
                end = match.end,
                plainLength = hidden,
            )
            val start = shown.length
            shown.append(text, match.start + rawHidden, match.end)
            links += match.copy(
                start = start,
                end = shown.length,
            )
            cursor = match.end
        }
        shown.append(text, cursor, text.length)
        return LinkedText(
            text = shown.toString(),
            links = links,
        )
    }

    /** Characters at the start of [url] that a base covers, or 0. Something must stay after the base. */
    private fun hiddenLength(url: String): Int {
        val schemeEnd = url.indexOf("://").let { if (it < 0) 0 else it + 3 }
        sortedBases.forEach { base ->
            val from = if (base.contains("://")) 0 else schemeEnd
            val end = from + base.length
            val isMatch = end < url.length &&
                url[end] in BOUNDARY &&
                url.regionMatches(
                    thisOffset = from,
                    other = base,
                    otherOffset = 0,
                    length = base.length,
                    ignoreCase = true,
                )
            if (isMatch) return end
        }
        return 0
    }

    private fun rawLength(
        raw: String,
        start: Int,
        end: Int,
        plainLength: Int,
    ): Int {
        var index = start
        var plain = 0
        while (plain < plainLength && index < end) {
            index += if (raw[index] == '\\' && index + 1 < end && raw[index + 1] == '/') 2 else 1
            plain++
        }
        return index - start
    }

    companion object {
        val None = UrlShortener(bases = emptyList())
        private const val BOUNDARY = "/?#"
    }
}
