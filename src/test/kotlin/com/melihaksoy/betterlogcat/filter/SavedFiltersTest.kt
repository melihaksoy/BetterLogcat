package com.melihaksoy.betterlogcat.filter

import com.melihaksoy.betterlogcat.ide.PackageAliasStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedFiltersTest {

    @Test
    fun `a filter round trips through its query string`() {
        val filter = LogFilter(
            terms = QueryParser.parseAll(text = "name:Network package:mine -tag~:art.* message:\"time out\"").terms,
            query = "retry",
        )

        val restored = LogFilter().withQueryString(text = filter.toQueryString())

        assertEquals(filter.terms, restored.effective().terms)
        assertEquals("retry", restored.effective().query)
    }

    @Test
    fun `name terms do not filter`() {
        val matcher = LogFilterMatcher(
            filter = LogFilter().withQueryString(text = "name:Network"),
            processNames = emptyMap(),
        )

        assertTrue(matcher.isQueryValid)
    }

    @Test
    fun `key list puts package mine right after package`() {
        val labels = QuerySuggestions.suggest(
            text = "",
            cursor = 0,
            context = SuggestionContext(
                packages = emptyList(),
                tags = emptyList(),
                processNames = emptyList(),
            ),
        ).items.map { it.label }

        assertEquals(
            listOf("age:", "context:", "is:", "level:", "line:", "message:", "method:", "name:", "package:", "package:mine", "pid:", "process:", "status:", "tag:"),
            labels,
        )
    }

    @Test
    fun `aliases replace the package and keep the process suffix`() {
        val aliases = mapOf("com.lesanciennes.app.debug" to "LA debug")

        assertEquals("LA debug", PackageAliasStore.display(processName = "com.lesanciennes.app.debug", aliases = aliases))
        assertEquals("LA debug:sync", PackageAliasStore.display(processName = "com.lesanciennes.app.debug:sync", aliases = aliases))
        assertEquals("com.android.systemui", PackageAliasStore.display(processName = "com.android.systemui", aliases = aliases))
    }
}
