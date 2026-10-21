package com.melihaksoy.betterlogcat.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TermFoldingTest {

    private val tags = "tag=:System.out,OkHttp,Choreographer,ActivityThread,ViewRootImpl"
    private val text = "package:mine $tags timeout "

    @Test
    fun `a long list shows its first values and a count`() {
        val fold = TermFolding.folds(text = text, cursor = -1).single()

        assertEquals("System.out,OkHttp +3", fold.text)
        assertEquals(tags.removePrefix("tag=:"), text.substring(fold.range))
    }

    @Test
    fun `the term under the cursor shows in full`() {
        val inside = text.indexOf("Choreographer")

        assertTrue(TermFolding.folds(text = text, cursor = inside).isEmpty())
        assertEquals(1, TermFolding.folds(text = text, cursor = text.length).size)
    }

    @Test
    fun `short terms never fold`() {
        assertTrue(TermFolding.folds(text = "tag=:a,b,c level:W", cursor = -1).isEmpty())
    }
}
