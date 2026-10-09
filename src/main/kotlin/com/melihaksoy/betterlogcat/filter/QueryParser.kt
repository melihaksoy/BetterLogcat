package com.melihaksoy.betterlogcat.filter

/** Terms pulled out of the search text, and the free text left over. */
data class ParsedQuery(
    val terms: List<QueryTerm>,
    val remainingText: String,
)

/**
 * Reads `key:value` terms from the search field. A term is complete once a space follows it,
 * so a half-typed term stays in the field. [parseAll] also takes the last term (for Enter).
 * Unknown keys stay free text.
 */
object QueryParser {

    private val termRegex = Regex(pattern = """(?<![^\s])(-?)([a-z]+)([=~]?):("[^"]*"|\S+)""")

    fun parseCompleted(text: String): ParsedQuery = parse(
        text = text,
        includeLast = false,
    )

    fun parseAll(text: String): ParsedQuery = parse(
        text = text,
        includeLast = true,
    )

    private fun parse(
        text: String,
        includeLast: Boolean,
    ): ParsedQuery {
        val terms = ArrayList<QueryTerm>()
        val remaining = StringBuilder()
        var cursor = 0
        termRegex.findAll(input = text).forEach { match ->
            val isComplete = match.range.last + 1 < text.length && text[match.range.last + 1].isWhitespace()
            val term = toTerm(match = match)
            if (term != null && (isComplete || includeLast)) {
                remaining.append(text, cursor, match.range.first)
                terms += term
                cursor = match.range.last + 1
            }
        }
        remaining.append(text, cursor, text.length)
        return ParsedQuery(
            terms = terms,
            remainingText = remaining.toString().replace(Regex(pattern = "\\s{2,}"), " ").trimStart(),
        )
    }

    /** Every term in [text] with its character range, for coloring the search field. */
    fun termRanges(text: String): List<Pair<IntRange, QueryTerm>> = termRegex.findAll(input = text)
        .mapNotNull { match -> toTerm(match = match)?.let { match.range to it } }
        .toList()

    private fun toTerm(match: MatchResult): QueryTerm? {
        val (negation, keyToken, modeToken, rawValue) = match.destructured
        val key = QueryKey.fromToken(token = keyToken) ?: return null
        val value = rawValue.removeSurrounding("\"")
        if (value.isEmpty()) return null
        return QueryTerm(
            key = key,
            value = value,
            mode = MatchMode.entries.first { it.token == modeToken },
            isNegated = negation == "-",
        )
    }
}
