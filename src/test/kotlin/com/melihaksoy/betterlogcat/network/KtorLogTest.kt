package com.melihaksoy.betterlogcat.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KtorLogTest {

    @Test
    fun `reads the method of a request and a response`() {
        assertEquals("GET", KtorLog.method(message = "REQUEST: https://pokeapi.co/api/v2/pokemon\nMETHOD: GET"))
        assertEquals("DELETE", KtorLog.method(message = "RESPONSE: 204\nMETHOD: DELETE\nFROM: https://pokeapi.co/api/v2/pokemon/1"))
    }

    @Test
    fun `reads the ktor 2 method form`() {
        assertEquals("POST", KtorLog.method(message = "REQUEST: https://pokeapi.co/api/v2/pokemon\nMETHOD: HttpMethod(value=POST)"))
    }

    @Test
    fun `other messages have no method`() {
        assertNull(KtorLog.method(message = "Basket loaded\nMETHOD: GET"))
        assertNull(KtorLog.method(message = "REQUEST: https://pokeapi.co/api/v2/pokemon"))
    }
}
