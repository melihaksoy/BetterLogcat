package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryTermsTest {

    private val names = mapOf(
        10 to "com.example.app",
        11 to "com.example.app:sync",
        20 to "com.android.systemui",
    )

    @Test
    fun `completed terms become chips and half typed ones stay`() {
        val parsed = QueryParser.parseCompleted(text = "package:mine -tag:art timeout tag:Ok")

        assertEquals(
            listOf(
                QueryTerm.Mine,
                QueryTerm(
                    key = QueryKey.TAG,
                    value = "art",
                    isNegated = true,
                ),
            ),
            parsed.terms,
        )
        assertEquals("timeout tag:Ok", parsed.remainingText)
    }

    @Test
    fun `enter takes the last term too and keeps unknown keys as text`() {
        val parsed = QueryParser.parseAll(text = "foo:bar tag~:Ok.* message:\"two words\"")

        assertEquals(listOf(MatchMode.REGEX, MatchMode.CONTAINS), parsed.terms.map { it.mode })
        assertEquals("two words", parsed.terms.last().value)
        assertEquals("foo:bar ", parsed.remainingText)
    }

    @Test
    fun `term text round trips`() {
        val term = QueryTerm(
            key = QueryKey.TAG,
            value = "Ok.*",
            mode = MatchMode.REGEX,
            isNegated = true,
        )

        assertEquals("-tag~:Ok.*", term.text)
        assertEquals(listOf(term), QueryParser.parseAll(text = term.text).terms)
    }

    @Test
    fun `same key terms combine with or, different keys with and`() {
        val matcher = matcher(
            terms = QueryParser.parseAll(text = "tag:OkHttp tag:Retrofit level:W").terms,
        )

        assertTrue(matcher.matches(entry = entry(tag = "OkHttp", level = LogLevel.WARN)))
        assertTrue(matcher.matches(entry = entry(tag = "Retrofit", level = LogLevel.ERROR)))
        assertFalse(matcher.matches(entry = entry(tag = "OkHttp", level = LogLevel.DEBUG)))
        assertFalse(matcher.matches(entry = entry(tag = "Choreographer", level = LogLevel.WARN)))
    }

    @Test
    fun `negated terms exclude`() {
        val matcher = matcher(terms = QueryParser.parseAll(text = "-tag:art -message:GC").terms)

        assertFalse(matcher.matches(entry = entry(tag = "art")))
        assertFalse(matcher.matches(entry = entry(message = "Background concurrent GC freed")))
        assertTrue(matcher.matches(entry = entry()))
    }

    @Test
    fun `package mine uses project ids and package names match sub processes`() {
        val mine = matcher(
            terms = listOf(QueryTerm.Mine),
            projectAppIds = setOf("com.example.app"),
        )
        val byName = matcher(terms = QueryParser.parseAll(text = "package:systemui").terms)

        assertTrue(mine.matches(entry = entry(pid = 11)))
        assertFalse(mine.matches(entry = entry(pid = 20)))
        assertTrue(byName.matches(entry = entry(pid = 20)))
        assertFalse(byName.matches(entry = entry(pid = 10)))
    }

    @Test
    fun `is crash and age terms`() {
        val crash = matcher(terms = QueryParser.parseAll(text = "is:crash").terms)
        val recent = LogFilterMatcher(
            filter = LogFilter(terms = QueryParser.parseAll(text = "age:5m").terms),
            processNames = names,
            nowEpochMillis = 1_791_540_000_000L + 60_000L,
        )

        assertTrue(crash.matches(entry = entry(tag = "AndroidRuntime", level = LogLevel.ERROR, message = "FATAL EXCEPTION: main")))
        assertFalse(crash.matches(entry = entry(tag = "AndroidRuntime", level = LogLevel.ERROR, message = "Shutting down VM")))
        assertTrue(recent.matches(entry = entry()))
        assertFalse(recent.matches(entry = entry(epochSeconds = 1_791_539_000L)))
    }

    @Test
    fun `bad regex term marks the query invalid and is ignored`() {
        val matcher = matcher(terms = QueryParser.parseAll(text = "tag~:Ok(").terms)

        assertFalse(matcher.isQueryValid)
        assertTrue(matcher.matches(entry = entry()))
    }

    @Test
    fun `picker and terms share one package filter`() {
        val filter = LogFilter(terms = listOf(QueryTerm(key = QueryKey.TAG, value = "art", isNegated = true)))
            .withPackageFilter(packageFilter = PackageFilter.ProjectApps)

        assertEquals(PackageFilter.ProjectApps, filter.packageFilter)
        assertEquals(PackageFilter.All, filter.withPackageFilter(packageFilter = PackageFilter.All).packageFilter)
        assertEquals("package:mine -tag:art ", filter.query)
        assertEquals(2, filter.effective().terms.size)
    }

    private fun matcher(
        terms: List<QueryTerm>,
        projectAppIds: Set<String> = emptySet(),
    ) = LogFilterMatcher(
        filter = LogFilter(terms = terms),
        processNames = names,
        projectAppIds = projectAppIds,
    )

    private fun entry(
        tag: String = "Garage",
        level: LogLevel = LogLevel.INFO,
        message: String = "Basket loaded",
        pid: Int = 10,
        epochSeconds: Long = 1_791_540_000L,
    ) = LogEntry(
        id = 1,
        epochSeconds = epochSeconds,
        nanos = 0,
        pid = pid,
        tid = pid,
        level = level,
        tag = tag,
        message = message,
    )
}
