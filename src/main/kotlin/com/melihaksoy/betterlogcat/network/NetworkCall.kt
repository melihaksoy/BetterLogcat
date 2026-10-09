package com.melihaksoy.betterlogcat.network

import androidx.compose.runtime.Immutable

/**
 * One HTTP call found in the logs. [statusCode] and [responseEntryId] stay 0 / [NO_ENTRY] while the
 * response has not arrived. [durationMillis] is -1 when unknown.
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

    companion object {
        const val NO_ENTRY = -1L
    }
}
