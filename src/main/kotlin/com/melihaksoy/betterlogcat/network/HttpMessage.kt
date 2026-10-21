package com.melihaksoy.betterlogcat.network

/**
 * What one log message says about an HTTP call, read from the message alone. Knows the same loggers as
 * [NetworkTracker]. An OkHttp response line does not name the method, so its [method] is null.
 * [statusStart] is where the status code starts in the message, or -1.
 */
data class HttpMessage(
    val isResponse: Boolean,
    val method: String?,
    val statusCode: Int?,
    val statusStart: Int = -1,
) {
    companion object {

        fun of(message: String): HttpMessage? = when {
            message.startsWith(KtorLog.REQUEST) -> HttpMessage(
                isResponse = false,
                method = KtorLog.method(message = message),
                statusCode = null,
            )
            message.startsWith(KtorLog.RESPONSE) -> {
                val match = ktorStatus.find(input = message)
                HttpMessage(
                    isResponse = true,
                    method = KtorLog.method(message = message),
                    statusCode = match?.value?.toInt(),
                    statusStart = match?.range?.first ?: -1,
                )
            }
            message.startsWith(OKHTTP_FAILED) -> HttpMessage(
                isResponse = true,
                method = null,
                statusCode = 0,
            )
            message.startsWith(OKHTTP_RESPONSE) -> okHttpStatus.find(input = message)?.let { match ->
                HttpMessage(
                    isResponse = true,
                    method = null,
                    statusCode = match.value.toInt(),
                    statusStart = match.range.first,
                )
            }
            message.startsWith(OKHTTP_REQUEST) -> okHttpRequest.find(input = message)?.let { match ->
                HttpMessage(
                    isResponse = false,
                    method = match.groupValues[1],
                    statusCode = null,
                )
            }
            else -> null
        }

        private const val OKHTTP_REQUEST = "--> "
        private const val OKHTTP_RESPONSE = "<-- "
        private const val OKHTTP_FAILED = "<-- HTTP FAILED: "
        private val ktorStatus = Regex(pattern = """(?<=^RESPONSE: )\d{3}\b""")
        private val okHttpStatus = Regex(pattern = """(?<=^<-- )\d{3}\b""")

        // `--> END GET` closes the request headers; it is not a new call.
        private val okHttpRequest = Regex(pattern = """^--> (?!END )([A-Z]+) """)
    }
}
