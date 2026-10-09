package com.melihaksoy.betterlogcat.format

import com.melihaksoy.betterlogcat.model.JsonContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonFormatterTest {

    @Test
    fun `pretty prints nested json`() {
        val pretty = JsonFormatter.format(
            json = """{"id":1,"tags":["a","b"],"price":{"amount":98500,"currency":"GBP"},"sold":false,"note":null}""",
            pretty = true,
        )

        val expected = """
            {
              "id": 1,
              "tags": [
                "a",
                "b"
              ],
              "price": {
                "amount": 98500,
                "currency": "GBP"
              },
              "sold": false,
              "note": null
            }
        """.trimIndent()
        assertEquals(expected, pretty)
    }

    @Test
    fun `minifies pretty json`() {
        val minified = JsonFormatter.format(
            json = "{\n  \"a\": [1, 2],\n  \"b\": \"x y\"\n}",
            pretty = false,
        )

        assertEquals("""{"a":[1,2],"b":"x y"}""", minified)
    }

    @Test
    fun `keeps escaped quotes inside strings`() {
        val minified = JsonFormatter.format(
            json = """{"quote":"he said \"hi\" {not json}"}""",
            pretty = false,
        )

        assertEquals("""{"quote":"he said \"hi\" {not json}"}""", minified)
    }

    @Test
    fun `rejects invalid json`() {
        assertNull(JsonFormatter.format(json = """{"a":1,}""", pretty = true))
        assertNull(JsonFormatter.format(json = """{a=1}""", pretty = true))
        assertNull(JsonFormatter.format(json = """{"a":1} trailing""", pretty = true))
    }

    @Test
    fun `detects json after a prefix`() {
        val content = JsonFormatter.detect(message = """<-- 200 https://api.example.com/v2/cars {"id":7,"make":"Jaguar"}""")

        assertTrue(content is JsonContent.Found)
        content as JsonContent.Found
        assertEquals("<-- 200 https://api.example.com/v2/cars ", content.prefix)
        assertEquals("""{"id":7,"make":"Jaguar"}""", content.minified)
    }

    @Test
    fun `skips a bracket that does not start json`() {
        val content = JsonFormatter.detect(message = """[OkHttp] response {"ok":true}""")

        assertTrue(content is JsonContent.Found)
        assertEquals("""{"ok":true}""", (content as JsonContent.Found).minified)
    }

    @Test
    fun `does not treat android bundles as json`() {
        assertEquals(JsonContent.None, JsonFormatter.detect(message = "Bundle[{id=7, make=Jaguar}]"))
        assertEquals(JsonContent.None, JsonFormatter.detect(message = "Loaded {} items"))
    }

    @Test
    fun `highlighter marks keys and values`() {
        val tokens = JsonHighlighter.tokens(json = """{"id": 7, "ok": true, "name": "E-Type"}""")

        assertEquals(
            listOf(
                JsonTokenKind.KEY,
                JsonTokenKind.NUMBER,
                JsonTokenKind.KEY,
                JsonTokenKind.KEYWORD,
                JsonTokenKind.KEY,
                JsonTokenKind.STRING,
            ),
            tokens.map { it.kind },
        )
    }
}
