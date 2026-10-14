package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.JsonContent
import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import com.melihaksoy.betterlogcat.model.ProblemKind
import java.util.regex.PatternSyntaxException

/** A [LogFilter] compiled once, then applied to many entries. */
class LogFilterMatcher(
    filter: LogFilter,
    private val processNames: Map<Int, String>,
    private val projectAppIds: Set<String> = emptySet(),
    private val nowEpochMillis: Long = System.currentTimeMillis(),
) {
    /** Terms live in the search text; parse them here, so every caller gets the same filter. */
    private val running: LogFilter = filter.effective()

    private val regex: Regex? = compileRegex()
    private val compiledTerms = running.terms.filter { it.key != QueryKey.NAME && it.key != QueryKey.CONTEXT }.map { CompiledTerm(term = it) }

    /** Lines to show before and after each match, from `context:N`. 0 when not set. */
    val contextLines: Int = running.terms
        .filter { it.key == QueryKey.CONTEXT && !it.isNegated }
        .mapNotNull { it.value.toIntOrNull()?.coerceIn(minimumValue = 0, maximumValue = MAX_CONTEXT) }
        .maxOrNull() ?: 0
    private val positiveByKey = compiledTerms.filter { !it.term.isNegated }.groupBy { it.term.key }
    private val negatives = compiledTerms.filter { it.term.isNegated }

    /** False when the free-text regex or a `~:` term does not compile. Those parts are ignored. */
    val isQueryValid: Boolean = (!running.isRegex || running.query.isEmpty() || regex != null) &&
        compiledTerms.all { it.isValid } &&
        running.terms.filter { it.key == QueryKey.CONTEXT }.all { it.value.toIntOrNull() != null }

    private val positiveTagTerms = positiveByKey[QueryKey.TAG].orEmpty()
    private val negativeTagTerms = negatives.filter { it.term.key == QueryKey.TAG }
    private val positivePackageTerms = positiveByKey[QueryKey.PACKAGE].orEmpty()
    private val negativePackageTerms = negatives.filter { it.term.key == QueryKey.PACKAGE }

    /** The filter has `package` terms, so lines of some processes never show. */
    val hasPackageTerms: Boolean = positivePackageTerms.isNotEmpty() || negativePackageTerms.isNotEmpty()

    fun matches(entry: LogEntry): Boolean {
        if (entry.level !in running.levels) return false
        for ((_, terms) in positiveByKey) {
            if (terms.none { it.matches(entry = entry) }) return false
        }
        if (negatives.any { it.matches(entry = entry) }) return false
        return matchesQuery(entry = entry)
    }

    /** True when the `tag` terms let [tag] through. Other terms do not count. For the Tags panel. */
    fun isTagVisible(tag: String): Boolean =
        (positiveTagTerms.isEmpty() || positiveTagTerms.any { it.matchesText(text = tag) }) &&
            negativeTagTerms.none { it.matchesText(text = tag) }

    /** True when the `package` terms let lines of [process] through. Other terms do not count. For the Tags panel. */
    fun isProcessVisible(process: String): Boolean =
        (positivePackageTerms.isEmpty() || positivePackageTerms.any { it.matchesProcess(process = process) }) &&
            negativePackageTerms.none { it.matchesProcess(process = process) }

    /** Character ranges in [text] that match the free-text query, for highlight. */
    fun highlights(text: String): List<IntRange> {
        if (running.query.isEmpty() || !isQueryValid) return emptyList()
        regex?.let { pattern ->
            return pattern.findAll(input = text).map { it.range }.filter { !it.isEmpty() }.take(MAX_HIGHLIGHTS).toList()
        }
        val ranges = ArrayList<IntRange>()
        var index = text.indexOf(
            string = running.query,
            ignoreCase = !running.isMatchCase,
        )
        while (index >= 0 && ranges.size < MAX_HIGHLIGHTS) {
            ranges += index until index + running.query.length
            index = text.indexOf(
                string = running.query,
                startIndex = index + running.query.length,
                ignoreCase = !running.isMatchCase,
            )
        }
        return ranges
    }

    private fun matchesQuery(entry: LogEntry): Boolean {
        val query = running.query.trim()
        if (query.isEmpty() || !isQueryValid) return true
        regex?.let { pattern ->
            return pattern.containsMatchIn(input = entry.message) || pattern.containsMatchIn(input = entry.tag)
        }
        return entry.message.contains(
            other = query,
            ignoreCase = !running.isMatchCase,
        ) || entry.tag.contains(
            other = query,
            ignoreCase = !running.isMatchCase,
        )
    }

    private fun compileRegex(): Regex? {
        val query = running.query.trim()
        if (!running.isRegex || query.isEmpty()) return null
        return try {
            if (running.isMatchCase) Regex(pattern = query) else Regex(pattern = query, option = RegexOption.IGNORE_CASE)
        } catch (_: PatternSyntaxException) {
            null
        }
    }

    private inner class CompiledTerm(val term: QueryTerm) {
        private val regex: Regex? = if (term.mode == MatchMode.REGEX) {
            try {
                Regex(pattern = term.value)
            } catch (_: PatternSyntaxException) {
                null
            }
        } else {
            null
        }
        /** `=:` takes a comma list: `tag=:OkHttp,Garage` is either tag. */
        private val exactValues: Set<String> = term.value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toHashSet()
        private val level: LogLevel? = parseLevel(value = term.value)
        private val ageMillis: Long? = parseAge(value = term.value)

        val isValid: Boolean = when {
            term.mode == MatchMode.REGEX && regex == null -> false
            term.key == QueryKey.LEVEL && level == null -> false
            term.key == QueryKey.AGE && ageMillis == null -> false
            else -> true
        }

        fun matches(entry: LogEntry): Boolean {
            if (!isValid) return !term.isNegated
            return when (term.key) {
                QueryKey.PACKAGE -> matchesPackage(entry = entry)
                QueryKey.TAG -> matchesText(text = entry.tag)
                QueryKey.MESSAGE -> matchesText(text = entry.message)
                QueryKey.LINE -> matchesText(text = "${processNames[entry.pid].orEmpty()} ${entry.level.letter} ${entry.tag}: ${entry.message}")
                QueryKey.NAME, QueryKey.CONTEXT -> true
                QueryKey.PROCESS -> matchesText(text = processNames[entry.pid].orEmpty())
                QueryKey.PID -> term.value.toIntOrNull() == entry.pid
                QueryKey.LEVEL -> level != null && entry.level >= level
                QueryKey.AGE -> ageMillis != null && entry.epochSeconds * 1_000 + entry.nanos / 1_000_000 >= nowEpochMillis - ageMillis
                QueryKey.IS -> matchesIs(entry = entry)
            }
        }

        // A start/death line belongs to the app it is about, not to system_server that logged it.
        private fun matchesPackage(entry: LogEntry): Boolean = matchesProcess(process = entry.processEvent?.packageName ?: processNames[entry.pid].orEmpty())

        /** Whether this `package` term matches [process], like [matches] does for a line of it. */
        fun matchesProcess(process: String): Boolean {
            if (!isValid) return !term.isNegated
            if (term.value == QueryTerm.MINE && term.mode == MatchMode.CONTAINS) {
                return projectAppIds.isEmpty() || process.substringBefore(':') in projectAppIds
            }
            return when (term.mode) {
                MatchMode.EXACT -> process.substringBefore(':') in exactValues
                else -> matchesText(text = process)
            }
        }

        fun matchesText(text: String): Boolean = when (term.mode) {
            MatchMode.CONTAINS -> text.contains(
                other = term.value,
                ignoreCase = true,
            )
            MatchMode.EXACT -> text in exactValues
            MatchMode.REGEX -> regex?.containsMatchIn(input = text) == true
        }

        private fun matchesIs(entry: LogEntry): Boolean = when (term.value.lowercase()) {
            "crash" -> entry.isCrash
            "anr" -> entry.problem == ProblemKind.ANR
            "exception" -> entry.problem == ProblemKind.EXCEPTION
            "strictmode" -> entry.problem == ProblemKind.STRICT_MODE
            "problem" -> entry.problem != null
            "stacktrace" -> entry.hasStackFrames
            "json" -> entry.json is JsonContent.Found
            else -> false
        }
    }

    private companion object {
        const val MAX_HIGHLIGHTS = 64
        const val MAX_CONTEXT = 50
        val ageRegex = Regex(pattern = """(\d+)([smhd])""")

        fun parseLevel(value: String): LogLevel? = LogLevel.entries.firstOrNull {
            it.name.equals(value, ignoreCase = true) || it.letter.toString().equals(value, ignoreCase = true)
        }

        fun parseAge(value: String): Long? {
            val match = ageRegex.matchEntire(input = value.lowercase()) ?: return null
            val amount = match.groupValues[1].toLongOrNull() ?: return null
            val unit = when (match.groupValues[2]) {
                "s" -> 1_000L
                "m" -> 60_000L
                "h" -> 3_600_000L
                else -> 86_400_000L
            }
            return amount * unit
        }
    }
}
