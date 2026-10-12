package com.melihaksoy.betterlogcat.network

import androidx.compose.runtime.Immutable

/**
 * One HTTP call found in the logs. [statusCode] and [responseEntryId] stay 0 / [NO_ENTRY] while the
 * response has not arrived. A failed call has a response entry and [statusCode] 0. [durationMillis] is
 * -1 when unknown.
 */
@Immutable
data class NetworkCall(
    val method: String,
    val url: String,
    val requestEntryId: Long,
    val requestEpochMillis: Long,
    val responseEntryId: Long = NO_ENTRY,
    val statusCode: Int = 0,
    val durationMillis: Long = -1,
) {
    val isPending: Boolean
        get() = responseEntryId == NO_ENTRY

    /** The address without scheme and host: `https://api.x.com/v1/items?id=2` → `/v1/items?id=2`. */
    val path: String
        get() {
            val afterScheme = url.substringAfter("://", missingDelimiterValue = url)
            val slash = afterScheme.indexOf('/')
            return if (slash < 0) afterScheme else afterScheme.substring(slash)
        }

    companion object {
        const val NO_ENTRY = -1L
    }
}
