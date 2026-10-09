package com.melihaksoy.betterlogcat.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StackFrameParserTest {

    @Test
    fun `finds java and kotlin frames`() {
        val text = """
            java.lang.IllegalStateException: boom
            	at com.vintagegarage.listing.ListingRepository.load(ListingRepository.kt:58)
            	at com.vintagegarage.listing.ListingRepository${'$'}fetch${'$'}1.invokeSuspend(ListingRepository.kt:71)
            	at android.os.Handler.dispatchMessage(Handler.java:106)
            	at java.lang.reflect.Method.invoke(Native Method)
        """.trimIndent()

        val frames = StackFrameParser.findFrames(text = text)

        assertEquals(3, frames.size)
        val first = frames.first()
        assertEquals("com.vintagegarage.listing.ListingRepository", first.className)
        assertEquals("ListingRepository.kt", first.fileName)
        assertEquals(58, first.line)
        assertEquals("ListingRepository.kt:58", text.substring(first.start, first.end))
        assertEquals("com.vintagegarage.listing.ListingRepository${'$'}fetch${'$'}1", frames[1].className)
    }

    @Test
    fun `ignores plain text`() {
        assertFalse(StackFrameParser.containsFrame(text = "Look at this (not a frame)"))
    }
}
