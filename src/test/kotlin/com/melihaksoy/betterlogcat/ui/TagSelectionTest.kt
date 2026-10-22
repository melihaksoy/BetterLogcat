package com.melihaksoy.betterlogcat.ui

import com.melihaksoy.betterlogcat.ui.logcat.model.TagSelectionUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class TagSelectionTest {

    private val rows = listOf("Adjust", "OkHttp", "art", "adbd", "Choreographer")

    private fun TagSelectionUiModel.click(
        tag: String,
        isToggle: Boolean = false,
        isRange: Boolean = false,
    ) = clicked(
        tag = tag,
        rows = rows,
        isToggle = isToggle,
        isRange = isRange,
    )

    @Test
    fun `a plain click selects only that tag`() {
        val selection = TagSelectionUiModel().click(tag = "art").click(tag = "OkHttp")

        assertEquals(setOf("OkHttp"), selection.tags)
    }

    @Test
    fun `a toggle click adds and removes one tag`() {
        val added = TagSelectionUiModel().click(tag = "art").click(tag = "Adjust", isToggle = true)
        val removed = added.click(tag = "art", isToggle = true)

        assertEquals(setOf("art", "Adjust"), added.tags)
        assertEquals(setOf("Adjust"), removed.tags)
    }

    @Test
    fun `a range click selects from the anchor, in both directions`() {
        val down = TagSelectionUiModel().click(tag = "OkHttp").click(tag = "adbd", isRange = true)
        val up = down.click(tag = "Adjust", isRange = true)

        assertEquals(setOf("OkHttp", "art", "adbd"), down.tags)
        // The anchor stays on the first click, so a second range starts from there too.
        assertEquals(setOf("Adjust", "OkHttp"), up.tags)
    }

    @Test
    fun `a toggle range click keeps the selection and adds the range`() {
        val selection = TagSelectionUiModel()
            .click(tag = "Choreographer")
            .click(tag = "Adjust", isToggle = true)
            .click(tag = "OkHttp", isToggle = true, isRange = true)

        assertEquals(setOf("Choreographer", "Adjust", "OkHttp"), selection.tags)
    }

    @Test
    fun `a drag moves the selection in row order, or the tag alone when it is not selected`() {
        val selection = TagSelectionUiModel().click(tag = "adbd").click(tag = "Adjust", isToggle = true)

        assertEquals(listOf("Adjust", "adbd"), selection.tagsFor(tag = "adbd", rows = rows))
        assertEquals(listOf("art"), selection.tagsFor(tag = "art", rows = rows))
        // A selected tag in a folded group is not on screen, so it does not move.
        assertEquals(listOf("Adjust"), selection.tagsFor(tag = "Adjust", rows = rows - "adbd"))
    }
}
