package com.melihaksoy.betterlogcat.network

import com.melihaksoy.betterlogcat.model.LogEntry

/**
 * Pairs HTTP requests with their responses as entries arrive. Knows two common loggers:
 * - Ktor `Logging`: `REQUEST: <url>` + `METHOD: GET`, then `RESPONSE: 200` + `METHOD:` + `FROM: <url>`.
 * - OkHttp `HttpLoggingInterceptor`: `--> GET <url>`, then `<-- 200 OK <url> (123ms)` or
 *   `<-- HTTP FAILED: <exception>. <url> (123ms)`. A failed call keeps status 0.
 * A response pairs with the oldest open request of the same method and address. Not thread safe.
 */
class NetworkTracker {

    private val calls = ArrayList<NetworkCall>()
    private val indexByEntry = HashMap<Long, Int>()

    /** Returns true when the call list changed. */
    fun onEntry(entry: LogEntry): Boolean {
        val message = entry.message
        return when {
            message.startsWith(KtorLog.REQUEST) -> addRequest(
                entry = entry,
                method = KtorLog.method(message = message) ?: return false,
                url = message.substringAfter(KtorLog.REQUEST).lineSequence().first().trim(),
            )
            message.startsWith(KtorLog.RESPONSE) -> addResponse(
                entry = entry,
                method = KtorLog.method(message = message) ?: return false,
                url = field(message = message, name = "FROM:") ?: return false,
                status = message.substringAfter(KtorLog.RESPONSE).trim().substringBefore(' ').substringBefore('\n').toIntOrNull() ?: 0,
                durationMillis = -1,
            )
            message.startsWith(OKHTTP_REQUEST) -> {
                val match = okHttpRequest.find(input = message) ?: return false
                addRequest(
                    entry = entry,
                    method = match.groupValues[1],
                    url = match.groupValues[2],
                )
            }
            message.startsWith(OKHTTP_FAILED) -> {
                val match = okHttpFailed.find(input = message) ?: return false
                addResponse(
                    entry = entry,
                    method = "",
                    url = match.groupValues[1],
                    status = 0,
                    durationMillis = match.groupValues[2].toLong(),
                )
            }
            message.startsWith(OKHTTP_RESPONSE) -> {
                val match = okHttpResponse.find(input = message) ?: return false
                addResponse(
                    entry = entry,
                    method = "",
                    url = match.groupValues[2],
                    status = match.groupValues[1].toInt(),
                    durationMillis = match.groupValues[3].toLongOrNull() ?: -1,
                )
            }
            else -> false
        }
    }

    fun snapshot(): List<NetworkCall> = ArrayList(calls)

    fun clear() {
        calls.clear()
        indexByEntry.clear()
    }

    private fun addRequest(
        entry: LogEntry,
        method: String,
        url: String,
    ): Boolean {
        if (url.isEmpty()) return false
        calls += NetworkCall(
            method = method,
            url = url,
            requestEntryId = entry.id,
            requestEpochMillis = entry.epochMillis(),
        )
        indexByEntry[entry.id] = calls.lastIndex
        trim()
        return true
    }

    private fun addResponse(
        entry: LogEntry,
        method: String,
        url: String,
        status: Int,
        durationMillis: Long,
    ): Boolean {
        val index = calls.indexOfFirst { it.isPending && it.url == url && (method.isEmpty() || it.method == method) }
        if (index < 0) {
            // Response without a logged request: still list it.
            calls += NetworkCall(
                method = method,
                url = url,
                requestEntryId = NetworkCall.NO_ENTRY,
                requestEpochMillis = entry.epochMillis(),
                responseEntryId = entry.id,
                statusCode = status,
                durationMillis = durationMillis,
            )
            indexByEntry[entry.id] = calls.lastIndex
            trim()
            return true
        }
        val request = calls[index]
        calls[index] = request.copy(
            responseEntryId = entry.id,
            statusCode = status,
            durationMillis = if (durationMillis >= 0) durationMillis else (entry.epochMillis() - request.requestEpochMillis).coerceAtLeast(minimumValue = 0),
        )
        indexByEntry[entry.id] = index
        return true
    }

    private fun trim() {
        if (calls.size <= MAX_CALLS) return
        val drop = calls.size - MAX_CALLS
        calls.subList(0, drop).clear()
        indexByEntry.clear()
        calls.forEachIndexed { index, call ->
            if (call.requestEntryId != NetworkCall.NO_ENTRY) indexByEntry[call.requestEntryId] = index
            if (call.responseEntryId != NetworkCall.NO_ENTRY) indexByEntry[call.responseEntryId] = index
        }
    }

    private fun field(
        message: String,
        name: String,
    ): String? = message.lineSequence().firstOrNull { it.startsWith(name) }?.substringAfter(name)?.trim()?.takeIf { it.isNotEmpty() }

    private fun LogEntry.epochMillis(): Long = epochSeconds * 1_000 + nanos / 1_000_000

    private companion object {
        const val MAX_CALLS = 1_000
        const val OKHTTP_REQUEST = "--> "
        const val OKHTTP_RESPONSE = "<-- "
        const val OKHTTP_FAILED = "<-- HTTP FAILED: "

        // `--> END GET` closes the request headers; it is not a new call.
        val okHttpRequest = Regex(pattern = """^--> (?!END )([A-Z]+) (\S+)""")
        val okHttpResponse = Regex(pattern = """^<-- (\d{3})(?: .*?)? (https?://\S+)(?: \((\d+)ms)?""")

        // OkHttp 4 leaves out the address and the duration, so such a line cannot pair with its request.
        val okHttpFailed = Regex(pattern = """^<-- HTTP FAILED: .*\. (https?://\S+) \((\d+)ms\)$""", option = RegexOption.MULTILINE)
    }
}
