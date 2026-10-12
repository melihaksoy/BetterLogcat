package com.melihaksoy.betterlogcat.network

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkCallTest {

    @Test
    fun `path drops scheme and host and keeps the query`() {
        assertEquals("/v1/items?id=2", call(url = "https://api.example.com/v1/items?id=2").path)
    }

    @Test
    fun `path of an address without a path is the host`() {
        assertEquals("api.example.com", call(url = "https://api.example.com").path)
    }

    @Test
    fun `path of an address without a scheme starts at the first slash`() {
        assertEquals("/v1/login", call(url = "api.example.com/v1/login").path)
    }

    private fun call(url: String) = NetworkCall(
        method = "GET",
        url = url,
        requestEntryId = 1,
        requestEpochMillis = 0,
    )
}
