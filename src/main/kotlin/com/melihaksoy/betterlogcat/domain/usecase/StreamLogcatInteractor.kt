package com.melihaksoy.betterlogcat.domain.usecase

import com.melihaksoy.betterlogcat.core.EitherFlow
import com.melihaksoy.betterlogcat.model.LogEntry

/** Streams log entries of one device in batches. Completes when the device goes away. */
interface StreamLogcatInteractor {
    operator fun invoke(request: LogcatRequest): EitherFlow<List<LogEntry>>
}

/**
 * Start point of the stream. [sinceEpochNanos] of 0 dumps the whole device buffer first, -1 shows only new entries.
 * Ids of new entries start above [afterId], so they stay above lines already in the log.
 */
data class LogcatRequest(
    val serial: String,
    val sinceEpochNanos: Long,
    val afterId: Long = 0L,
)
