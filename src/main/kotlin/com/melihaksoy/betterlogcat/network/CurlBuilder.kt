package com.melihaksoy.betterlogcat.network

/**
 * Rebuilds a cURL command from a logged request. Reads Ktor's request format (`REQUEST:`, `METHOD:`,
 * `-> Header: value` lines, `BODY START` ... `BODY END`) and OkHttp's first line (`--> POST <url>`).
 */
object CurlBuilder {

    private val skippedHeaders = setOf("content-length", "host", "accept-encoding")

    fun fromMessage(message: String): String? {
        val lines = message.lines()
        val first = lines.firstOrNull() ?: return null
        val (method, url) = when {
            first.startsWith("REQUEST:") -> {
                val method = lines.firstOrNull { it.startsWith("METHOD:") }?.substringAfter("METHOD:")?.trim() ?: "GET"
                method to first.substringAfter("REQUEST:").trim()
            }
            first.startsWith("--> ") -> {
                val parts = first.removePrefix("--> ").split(' ')
                if (parts.size < 2) return null
                parts[0] to parts[1]
            }
            else -> return null
        }
        val headers = lines
            .filter { it.startsWith("-> ") && it.contains(':') }
            .map { it.removePrefix("-> ") }
            .filter { it.substringBefore(':').trim().lowercase() !in skippedHeaders }
        val body = lines
            .dropWhile { !it.startsWith("BODY START") }
            .drop(n = 1)
            .takeWhile { !it.startsWith("BODY END") }
            .joinToString(separator = "\n")
            .trim()
        return buildString {
            append("curl -X ").append(method).append(' ').append(quote(text = url))
            headers.forEach { append(" \\\n  -H ").append(quote(text = it.trim())) }
            if (body.isNotEmpty()) append(" \\\n  --data-raw ").append(quote(text = body))
        }
    }

    /** Single-quoted for a POSIX shell. */
    private fun quote(text: String): String = "'" + text.replace("'", "'\\''") + "'"
}
