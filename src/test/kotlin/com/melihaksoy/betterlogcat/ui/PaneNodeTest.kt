package com.melihaksoy.betterlogcat.ui

import com.melihaksoy.betterlogcat.toolwindow.PaneNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaneNodeTest {

    @Test
    fun `nested splits round trip with any filter text`() {
        val layout = PaneNode.Split(
            isVertical = false,
            proportion = 0.4f,
            first = PaneNode.Leaf(
                serial = "emulator-5554",
                query = "package:mine -tag:art message:\"time: out\" ",
            ),
            second = PaneNode.Split(
                isVertical = true,
                proportion = 0.5f,
                first = PaneNode.Leaf(serial = "38211FDJH00A4K", query = ""),
                second = PaneNode.Leaf(serial = "", query = "is:crash"),
            ),
        )

        assertEquals(layout, PaneNode.decode(text = layout.encode()))
    }

    @Test
    fun `broken input gives null`() {
        assertNull(PaneNode.decode(text = "S:v:0.5:broken"))
        assertNull(PaneNode.decode(text = "nonsense"))
    }
}
