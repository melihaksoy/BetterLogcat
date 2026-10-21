package com.melihaksoy.betterlogcat.network

/**
 * Reads Ktor `Logging` messages: `REQUEST: <url>` or `RESPONSE: 200`, then `METHOD: GET`. Ktor 2 writes
 * the method as `HttpMethod(value=GET)`.
 */
object KtorLog {

    const val REQUEST = "REQUEST:"
    const val RESPONSE = "RESPONSE:"
    private const val METHOD = "METHOD:"

    /** The method of a Ktor request or response, as `GET`. Null for other messages. */
    fun method(message: String): String? {
        if (!message.startsWith(REQUEST) && !message.startsWith(RESPONSE)) return null
        return message.lineSequence()
            .firstOrNull { it.startsWith(METHOD) }
            ?.substringAfter(METHOD)
            ?.trim()
            ?.removePrefix("HttpMethod(value=")
            ?.removeSuffix(")")
            ?.takeIf { it.isNotEmpty() }
    }
}
