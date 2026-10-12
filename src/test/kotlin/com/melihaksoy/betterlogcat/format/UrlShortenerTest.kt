package com.melihaksoy.betterlogcat.format

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlShortenerTest {

    private val shortener = UrlShortener(bases = listOf("https://staging.example.com"))

    @Test
    fun `base is hidden and the link keeps the full address`() {
        val shown = shortener.shorten(text = "REQUEST: https://staging.example.com/app-api/v1/push-tokens")

        assertEquals("REQUEST: /app-api/v1/push-tokens", shown.text)
        val link = shown.links.single()
        assertEquals("https://staging.example.com/app-api/v1/push-tokens", link.url)
        assertEquals("/app-api/v1/push-tokens", shown.text.substring(link.start, link.end))
    }

    @Test
    fun `other hosts stay as they are`() {
        val text = "FROM: https://api.other.com/v1/items and https://staging.example.com.evil.io/x"

        assertEquals(text, shortener.shorten(text = text).text)
    }

    @Test
    fun `a base alone stays, so the link never goes empty`() {
        val text = "host https://staging.example.com"

        assertEquals(text, shortener.shorten(text = text).text)
    }

    @Test
    fun `a base without a scheme and with a trailing slash matches`() {
        val shown = UrlShortener(bases = listOf("staging.example.com/")).shorten(text = "http://staging.example.com/a?b=1")

        assertEquals("/a?b=1", shown.text)
    }

    @Test
    fun `the longest base wins`() {
        val shown = UrlShortener(bases = listOf("https://staging.example.com", "https://staging.example.com/app-api"))
            .shorten(text = "https://staging.example.com/app-api/v1/user")

        assertEquals("/v1/user", shown.text)
    }

    @Test
    fun `json escaped slashes are hidden too`() {
        val shown = shortener.shorten(text = """{"url":"https:\/\/staging.example.com\/app-api\/v1"}""")

        assertEquals("""{"url":"\/app-api\/v1"}""", shown.text)
        assertEquals("https://staging.example.com/app-api/v1", shown.links.single().url)
    }

    @Test
    fun `offsets of later links move with the hidden text`() {
        val shown = shortener.shorten(text = "a https://staging.example.com/one b https://x.io/two")

        assertEquals("a /one b https://x.io/two", shown.text)
        assertEquals(listOf("/one", "https://x.io/two"), shown.links.map { shown.text.substring(it.start, it.end) })
    }
}
