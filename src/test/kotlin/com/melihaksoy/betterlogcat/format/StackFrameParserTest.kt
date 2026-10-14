package com.melihaksoy.betterlogcat.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StackFrameParserTest {

    @Test
    fun `finds java and kotlin frames`() {
        val text = """
            java.lang.IllegalStateException: boom
            	at com.example.pokedex.data.PokemonRepository.load(PokemonRepository.kt:58)
            	at com.example.pokedex.data.PokemonRepository${'$'}fetch${'$'}1.invokeSuspend(PokemonRepository.kt:71)
            	at android.os.Handler.dispatchMessage(Handler.java:106)
            	at java.lang.reflect.Method.invoke(Native Method)
        """.trimIndent()

        val frames = StackFrameParser.findFrames(text = text)

        assertEquals(3, frames.size)
        val first = frames.first()
        assertEquals("com.example.pokedex.data.PokemonRepository", first.className)
        assertEquals("PokemonRepository.kt", first.fileName)
        assertEquals(58, first.line)
        assertEquals("PokemonRepository.kt:58", text.substring(first.start, first.end))
        assertEquals("com.example.pokedex.data.PokemonRepository${'$'}fetch${'$'}1", frames[1].className)
    }

    @Test
    fun `ignores plain text`() {
        assertFalse(StackFrameParser.containsFrame(text = "Look at this (not a frame)"))
    }
}
