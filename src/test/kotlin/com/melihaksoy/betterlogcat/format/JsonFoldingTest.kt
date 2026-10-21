package com.melihaksoy.betterlogcat.format

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonFoldingTest {

    private val json = """
        {
          "data": {
            "id": 25,
            "name": "pikachu, {not a block}",
            "types": [
              "electric"
            ],
            "stats": [1, 2, 3]
          },
          "empty": {}
        }
    """.trimIndent()

    @Test
    fun `only blocks that span lines fold`() {
        val regions = JsonFolding.regions(text = json)

        assertEquals(listOf("{", "{", "["), regions.map { json[it.open].toString() })
        assertEquals(listOf(2, 4, 1), regions.map { it.children })
        assertEquals(listOf(false, false, true), regions.map { it.isArray })
        assertEquals(listOf(0, 0, 2), regions.map { it.index })
    }

    @Test
    fun `a region closes on its own bracket`() {
        val types = JsonFolding.regions(text = json).last()

        assertEquals(']', json[types.close])
        assertEquals("[\n      \"electric\"\n    ]", json.substring(types.open, types.close + 1))
    }
}
