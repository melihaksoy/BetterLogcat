package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.model.LogEntry
import com.melihaksoy.betterlogcat.model.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFilterTest {

    private val names = mapOf(
        10 to "com.example.pokedex",
        11 to "com.example.pokedex:sync",
        20 to "com.android.systemui",
    )

    @Test
    fun `levels toggle one by one`() {
        val matcher = matcher(filter = LogFilter(levels = setOf(LogLevel.DEBUG, LogLevel.ERROR)))

        assertTrue(matcher.matches(entry = entry(level = LogLevel.DEBUG)))
        assertFalse(matcher.matches(entry = entry(level = LogLevel.INFO)))
        assertFalse(matcher.matches(entry = entry(level = LogLevel.WARN)))
        assertTrue(matcher.matches(entry = entry(level = LogLevel.ERROR)))
    }

    @Test
    fun `package filter includes sub processes`() {
        val matcher = matcher(filter = LogFilter().withPackageFilter(packageFilter = PackageFilter.Package(name = "com.example.pokedex")))

        assertTrue(matcher.matches(entry = entry(pid = 10)))
        assertTrue(matcher.matches(entry = entry(pid = 11)))
        assertFalse(matcher.matches(entry = entry(pid = 20)))
        assertFalse(matcher.matches(entry = entry(pid = 99)))
    }

    @Test
    fun `project apps filter uses project ids and shows all while none are known`() {
        val filter = LogFilter(terms = listOf(QueryTerm.Mine))
        val withIds = LogFilterMatcher(
            filter = filter,
            processNames = names,
            projectAppIds = setOf("com.example.pokedex"),
        )
        val withoutIds = matcher(filter = filter)

        assertTrue(withIds.matches(entry = entry(pid = 11)))
        assertFalse(withIds.matches(entry = entry(pid = 20)))
        assertTrue(withoutIds.matches(entry = entry(pid = 20)))
    }

    @Test
    fun `text query matches message or tag ignoring case`() {
        val matcher = matcher(filter = LogFilter(query = "okhttp"))

        assertTrue(matcher.matches(entry = entry(tag = "OkHttp")))
        assertTrue(matcher.matches(entry = entry(message = "via OKHTTP client")))
        assertFalse(matcher.matches(entry = entry()))
    }

    @Test
    fun `invalid regex is reported and ignored`() {
        val matcher = matcher(
            filter = LogFilter(
                query = "user(id=",
                isRegex = true,
            ),
        )

        assertFalse(matcher.isQueryValid)
        assertTrue(matcher.matches(entry = entry()))
    }

    @Test
    fun `exact tag terms take comma lists`() {
        val matcher = matcher(filter = LogFilter(query = "tag=:Garage,Choreographer -tag=:Choreographer "))

        assertTrue(matcher.matches(entry = entry(tag = "Garage")))
        assertFalse(matcher.matches(entry = entry(tag = "Choreographer")))
        assertFalse(matcher.matches(entry = entry(tag = "OkHttp")))
    }

    @Test
    fun `highlights every match`() {
        val matcher = matcher(filter = LogFilter(query = "car"))

        assertEquals(listOf(0..2, 8..10), matcher.highlights(text = "Car and car"))
    }

    @Test
    fun `buffer trims oldest entries and keeps the filtered view in sync`() {
        val buffer = LogBuffer(capacity = 1_000)
        buffer.refilter(predicate = { it.level >= LogLevel.WARN })

        buffer.append(entries = (1L..1_500L).map { entry(id = it, level = if (it % 2 == 0L) LogLevel.WARN else LogLevel.DEBUG) })

        val visible = buffer.visibleSnapshot()
        assertTrue(buffer.totalCount <= 1_000)
        assertTrue(visible.all { it.level == LogLevel.WARN })
        assertEquals(1_500L, visible.last().id)
        assertTrue(visible.first().id > 500L)
    }

    private fun matcher(filter: LogFilter) = LogFilterMatcher(
        filter = filter,
        processNames = names,
    )

    private fun entry(
        id: Long = 1,
        level: LogLevel = LogLevel.INFO,
        tag: String = "Garage",
        message: String = "Basket loaded",
        pid: Int = 10,
    ) = LogEntry(
        id = id,
        epochSeconds = 1_791_540_000L,
        nanos = 0,
        pid = pid,
        tid = pid,
        level = level,
        tag = tag,
        message = message,
    )
}
