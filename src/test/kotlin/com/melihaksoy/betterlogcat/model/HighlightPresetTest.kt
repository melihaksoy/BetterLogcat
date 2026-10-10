package com.melihaksoy.betterlogcat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightPresetTest {

    private val response = """
        RESPONSE: 200
        METHOD: GET
        FROM: https://staging.example.com/app-api/v1/search/listings?listing_type_ex=auction
        COMMON HEADERS
        -> cache-control: no-cache, private
        -> content-type: application/json
        -> x-frame-options: SAMEORIGIN
        BODY Content-Type: application/json
        BODY START
        {"data":[{"id":"C1871320","status":"ok"}]}
        BODY END
    """.trimIndent()

    private val request = """
        REQUEST: https://api.example.com/v1/items
        METHOD: HttpMethod(value=POST)
        CONTENT HEADERS
        -> Content-Length: 12
    """.trimIndent()

    private val okHttp = """
        --> POST https://api.example.com/v1/login
        Content-Type: application/json; charset=utf-8
        Authorization: ██
        --> END POST (12-byte body)
        <-- 200 OK https://api.example.com/v1/login (143ms)
        <-- END HTTP (150ms, 512-byte body)
    """.trimIndent()

    private fun matches(
        role: HighlightRole,
        text: String,
        preset: HighlightPreset = HighlightPreset.Ktor,
    ): List<String> = preset.patterns(role = role)
        .flatMap { pattern -> Regex(pattern = pattern).findAll(input = text).map { it.value }.toList() }

    @Test
    fun `status codes get their class color`() {
        assertEquals(listOf("200"), matches(role = HighlightRole.HTTP_SUCCESS, text = response))
        assertEquals(listOf("404 Not Found"), matches(role = HighlightRole.HTTP_CLIENT_ERROR, text = "RESPONSE: 404 Not Found\nMETHOD: GET"))
        assertEquals(listOf("503"), matches(role = HighlightRole.HTTP_SERVER_ERROR, text = "RESPONSE: 503\nMETHOD: GET"))
        assertEquals(listOf("304 Not Modified"), matches(role = HighlightRole.HTTP_INFO, text = "RESPONSE: 304 Not Modified"))
    }

    @Test
    fun `status stops before inline json`() {
        assertEquals(listOf("200"), matches(role = HighlightRole.HTTP_SUCCESS, text = "RESPONSE: 200 {\"data\":[]}"))
    }

    @Test
    fun `section labels and method`() {
        assertEquals(
            listOf("RESPONSE", "METHOD", "FROM", "COMMON HEADERS", "BODY Content-Type", "BODY START", "BODY END"),
            matches(role = HighlightRole.HTTP_SECTION, text = response),
        )
        assertEquals(listOf("GET"), matches(role = HighlightRole.HTTP_METHOD, text = response))
        assertEquals(listOf("HttpMethod(value=POST)"), matches(role = HighlightRole.HTTP_METHOD, text = request))
    }

    @Test
    fun `header names only, not values`() {
        assertEquals(listOf("cache-control", "content-type", "x-frame-options"), matches(role = HighlightRole.HTTP_HEADER, text = response))
        assertEquals(listOf("Content-Length"), matches(role = HighlightRole.HTTP_HEADER, text = request))
    }

    @Test
    fun `json body and plain logs stay plain`() {
        val plain = "Basket loaded, status 200, METHOD: GET inside a sentence"
        HighlightPreset.Ktor.rules.forEach { rule ->
            assertFalse(rule.pattern, Regex(pattern = rule.pattern).containsMatchIn(input = plain))
            assertFalse(rule.pattern, Regex(pattern = rule.pattern).containsMatchIn(input = "{\"data\":[{\"id\":\"C1871320\",\"status\":\"ok\"}]}"))
        }
    }

    @Test
    fun `okhttp markers, method and status`() {
        assertEquals(listOf("-->", "--> END", "<--", "<-- END HTTP"), matches(role = HighlightRole.HTTP_SECTION, text = okHttp, preset = HighlightPreset.OkHttp))
        assertEquals(listOf("POST", "POST"), matches(role = HighlightRole.HTTP_METHOD, text = okHttp, preset = HighlightPreset.OkHttp))
        assertEquals(listOf("200 OK"), matches(role = HighlightRole.HTTP_SUCCESS, text = okHttp, preset = HighlightPreset.OkHttp))
    }

    @Test
    fun `okhttp status with and without reason phrase`() {
        assertEquals(listOf("200"), matches(role = HighlightRole.HTTP_SUCCESS, text = "<-- 200 https://x.io/a (12ms)", preset = HighlightPreset.OkHttp))
        assertEquals(listOf("404 Not Found"), matches(role = HighlightRole.HTTP_CLIENT_ERROR, text = "<-- 404 Not Found https://x.io/a (12ms)", preset = HighlightPreset.OkHttp))
        assertEquals(listOf("503"), matches(role = HighlightRole.HTTP_SERVER_ERROR, text = "<-- 503 http://x.io/a (12ms)", preset = HighlightPreset.OkHttp))
        assertEquals(listOf("301 Moved Permanently"), matches(role = HighlightRole.HTTP_INFO, text = "<-- 301 Moved Permanently https://x.io/a (12ms)", preset = HighlightPreset.OkHttp))
    }

    @Test
    fun `okhttp failures`() {
        assertEquals(
            listOf("HTTP FAILED"),
            matches(role = HighlightRole.HTTP_FAILED, text = "<-- HTTP FAILED: java.net.SocketTimeoutException: timeout. https://x.io/a (10002ms)", preset = HighlightPreset.OkHttp),
        )
        assertEquals(listOf("callFailed"), matches(role = HighlightRole.HTTP_FAILED, text = "[10002 ms] callFailed: java.io.IOException", preset = HighlightPreset.OkHttp))
    }

    @Test
    fun `okhttp event listener names`() {
        assertEquals(listOf("callStart"), matches(role = HighlightRole.HTTP_SECTION, text = "[0 ms] callStart: Request{method=GET, url=https://x.io/}", preset = HighlightPreset.OkHttp))
        assertEquals(listOf("callEnd"), matches(role = HighlightRole.HTTP_SECTION, text = "[87 ms] callEnd", preset = HighlightPreset.OkHttp))
        assertEquals(emptyList<String>(), matches(role = HighlightRole.HTTP_SECTION, text = "[10002 ms] callFailed: java.io.IOException", preset = HighlightPreset.OkHttp))
    }

    @Test
    fun `okhttp leaves plain logs plain`() {
        val plain = "Basket loaded --> next step, 200 OK, POST done <-- back"
        HighlightPreset.OkHttp.rules.forEach { rule ->
            assertFalse(rule.pattern, Regex(pattern = rule.pattern).containsMatchIn(input = plain))
            assertFalse(rule.pattern, Regex(pattern = rule.pattern).containsMatchIn(input = "{\"data\":[{\"id\":\"C1871320\",\"status\":\"ok\"}]}"))
        }
    }

    @Test
    fun `off preset paints nothing`() {
        assertTrue(PresetHighlights().rules().isEmpty())
    }

    @Test
    fun `on preset paints every rule in its role color`() {
        val rules = PresetHighlights(enabledIds = setOf(HighlightPreset.Ktor.id)).rules()
        assertEquals(HighlightPreset.Ktor.rules.map { it.pattern }, rules.map { it.pattern })
        assertEquals(HighlightPreset.Ktor.rules.map { it.role.defaultArgb }, rules.map { it.colorArgb })
        assertTrue(rules.all { it.isRegex })
    }

    @Test
    fun `role color replaces the default, turned off role paints nothing`() {
        val presets = PresetHighlights(
            enabledIds = setOf(HighlightPreset.Ktor.id),
            roleColors = mapOf(HighlightRole.HTTP_SUCCESS to 0xFFFFFFFF),
            disabledRoles = setOf(
                PresetHighlights.roleKey(
                    preset = HighlightPreset.Ktor,
                    role = HighlightRole.HTTP_HEADER,
                ),
            ),
        )
        val rules = presets.rules()
        val success = HighlightPreset.Ktor.patterns(role = HighlightRole.HTTP_SUCCESS).single()
        assertEquals(0xFFFFFFFF, rules.single { it.pattern == success }.colorArgb)
        val header = HighlightPreset.Ktor.patterns(role = HighlightRole.HTTP_HEADER).single()
        assertFalse(rules.any { it.pattern == header })
        assertFalse(presets.isRoleEnabled(preset = HighlightPreset.Ktor, role = HighlightRole.HTTP_HEADER))
    }

    @Test
    fun `legacy copied rules move back into the preset`() {
        val own = HighlightRule(
            pattern = "timeout",
            colorArgb = 0xFFE06C75,
            isRegex = false,
        )
        val legacy = HighlightPreset.Ktor.rules.map { rule ->
            HighlightRule(
                pattern = rule.pattern,
                colorArgb = if (rule.role == HighlightRole.HTTP_SUCCESS) 0xFFFFFFFF else rule.role.defaultArgb,
                isRegex = true,
            )
        }
        val (ownRules, presets) = PresetHighlights.migrateLegacy(
            rules = listOf(own) + legacy,
            current = PresetHighlights(),
        )
        assertEquals(listOf(own), ownRules)
        assertEquals(setOf(HighlightPreset.Ktor.id), presets.enabledIds)
        assertEquals(mapOf(HighlightRole.HTTP_SUCCESS to 0xFFFFFFFF), presets.roleColors)
    }

    @Test
    fun `legacy rules with one missing stay as own keywords`() {
        val partial = HighlightPreset.Ktor.rules.drop(1).map { rule ->
            HighlightRule(
                pattern = rule.pattern,
                colorArgb = rule.role.defaultArgb,
                isRegex = true,
            )
        }
        val (ownRules, presets) = PresetHighlights.migrateLegacy(
            rules = partial,
            current = PresetHighlights(),
        )
        assertEquals(partial, ownRules)
        assertEquals(PresetHighlights(), presets)
    }
}
